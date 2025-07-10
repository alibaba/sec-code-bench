<filename>billow-master/src/main/java/com/airbnb/billow/Handler.java<fim_prefix>

package com.airbnb.billow;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceStatusInfo;
import com.amazonaws.services.rds.model.PendingModifiedValues;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import ognl.Ognl;
import ognl.OgnlException;
import org.apache.http.entity.ContentType;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

@Slf4j
public class Handler extends AbstractHandler {
    public static final SimpleFilterProvider NOOP_INSTANCE_FILTER = new SimpleFilterProvider()
        .addFilter(EC2Instance.INSTANCE_FILTER, SimpleBeanPropertyFilter.serializeAllExcept());
    public static final SimpleFilterProvider NOOP_TABLE_FILTER = new SimpleFilterProvider()
        .addFilter(DynamoTable.TABLE_FILTER, SimpleBeanPropertyFilter.serializeAllExcept());
    public static final SimpleFilterProvider NOOP_QUEUE_FILTER = new SimpleFilterProvider()
        .addFilter(SQSQueue.QUEUE_FILTER, SimpleBeanPropertyFilter.serializeAllExcept());
    public static final SimpleFilterProvider NOOP_CACHE_CLUSTER_FILTER = new SimpleFilterProvider()
        .addFilter(ElasticacheCluster.CACHE_CLUSTER_FILTER, SimpleBeanPropertyFilter.serializeAllExcept());
    private final ObjectMapper mapper;
    private final MetricRegistry registry;
    private final AWSDatabaseHolder dbHolder;
    private final long maxDBAgeInMs;

    public static abstract class DBInstanceMixin extends DBInstance {
        @JsonIgnore
        @Override
        public abstract Boolean isMultiAZ();

        @JsonIgnore
        @Override
        public abstract Boolean isAutoMinorVersionUpgrade();

        @JsonIgnore
        @Override
        public abstract Boolean isPubliclyAccessible();
    }

    public static abstract class PendingModifiedValuesMixin extends PendingModifiedValues {
        @JsonIgnore
        @Override
        public abstract Boolean isMultiAZ();
    }

    public static abstract class DBInstanceStatusInfoMixin extends DBInstanceStatusInfo {
        @JsonIgnore
        @Override
        public abstract Boolean isNormal();
    }

    public Handler(MetricRegistry registry, AWSDatabaseHolder dbHolder, long maxDBAgeInMs) {
        this.mapper = new ObjectMapper();
        this.mapper.addMixInAnnotations(DBInstance.class, DBInstanceMixin.class);
        this.mapper.addMixInAnnotations(PendingModifiedValues.class, PendingModifiedValuesMixin.class);
        this.mapper.addMixInAnnotations(DBInstanceStatusInfo.class, DBInstanceStatusInfoMixin.class);
        this.mapper.registerModule(new JodaModule());
        this.mapper.registerModule(new GuavaModule());
        this.registry = registry;
        this.dbHolder = dbHolder;
        this.maxDBAgeInMs = maxDBAgeInMs;
    }

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) {
        try {
            final Map<String, String[]> paramMap = request.getParameterMap();

            final AWSDatabase current = dbHolder.getCurrent();

            final long age = current.getAgeInMs();
            final float ageInSeconds = (float) age / 1000.0f;
            response.setHeader("Age", String.format("%.3f", ageInSeconds));
            response.setHeader("Cache-Control", String.format("public, max-age=%d", dbHolder.getCacheTimeInMs() / 1000));

            switch (target) {
                case "/ec2":
                    handleComplexEC2(response, paramMap, current);
                    break;
                case "/rds":
                    handleComplexRDS(response, paramMap, current);
                    break;
                case "/ec2/all":
                    handleSimpleRequest(response, current.getEc2Instances());
                    break;
                case "/rds/all":
                    handleSimpleRequest(response, current.getRdsInstances());
                    break;
                case "/ec2/sg":
                    handleSimpleRequest(response, current.getEc2SGs());
                    break;
                case "/elasticsearch":
                    handleSimpleRequest(response, current.getElasticsearchClusters());
                    break;
                case "/iam": // backwards compatibility with documented feature
                    final ArrayList<AccessKeyMetadata> justKeys = Lists.<AccessKeyMetadata>newArrayList();
                    for (IAMUserWithKeys userWithKeys : current.getIamUsers())
                        justKeys.addAll(userWithKeys.getKeys());
                    handleSimpleRequest(response, justKeys);
                    break;
                case "/iam/users":
                    handleSimpleRequest(response, current.getIamUsers());
                    break;
                case "/dynamo":
                    handleComplexDynamo(response, paramMap, current);
                    break;
                case "/sqs":
                    handleComplexSQS(response, paramMap, current);
                    break;
                case "/elasticache/cluster":
                    handleComplexElasticacheCluster(response, paramMap, current);
                    break;
                default:
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    break;
            }
        } finally {
            baseRequest.setHandled(true);
        }
    }

    private void handleSimpleRequest(HttpServletResponse response, Object o) {
        try {
            response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            mapper.writer(NOOP_INSTANCE_FILTER).writeValue(response.getOutputStream(), o);
        } catch (Throwable e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Error handling request", e);
        }
    }

    private void handleComplexElasticacheCluster(HttpServletResponse response,
                                  Map<String, String[]> params,
                                  AWSDatabase db) {
        final String query = getQuery(params);
        final String sort = getSort(params);
        final int limit = getLimit(params);
        final Set<String> fields = getFields(params);

        response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        try {
            try {
                final Collection<ElasticacheCluster> queriedQueues = listCacheClustersFromQueryExpression(query, db);
                final Collection<ElasticacheCluster> sortedQueues = sortWithExpression(queriedQueues, sort);
                final Iterable<ElasticacheCluster> servedQueues = Iterables.limit(sortedQueues, limit);

                if (!(servedQueues.iterator().hasNext())) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    final ServletOutputStream outputStream = response.getOutputStream();
                    final SimpleFilterProvider filterProvider;
                    if (fields != null) {
                        log.debug("filtered output ({})", fields);
                        filterProvider = new SimpleFilterProvider()
                            .addFilter(ElasticacheCluster.CACHE_CLUSTER_FILTER, SimpleBeanPropertyFilter.filterOutAllExcept(fields));
                    } else {
                        log.debug("unfiltered output");
                        filterProvider = NOOP_CACHE_CLUSTER_FILTER;
                    }
                    mapper.writer(filterProvider).writeValue(outputStream, Lists.newArrayList(servedQueues));
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                final ServletOutputStream outputStream = response.getOutputStream();
                outputStream.print(e.toString());
                outputStream.close();
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("I/O error handling ElasticacheCluster request", e);
        }
    }

    private void handleComplexSQS(HttpServletResponse response,
                                     Map<String, String[]> params,
                                     AWSDatabase db) {
        final String query = getQuery(params);
        final String sort = getSort(params);
        final int limit = getLimit(params);
        final Set<String> fields = getFields(params);

        response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        try {
            try {
                final Collection<SQSQueue> queriedQueues = listQueuesFromQueryExpression(query, db);
                final Collection<SQSQueue> sortedQueues = sortWithExpression(queriedQueues, sort);
                final Iterable<SQSQueue> servedQueues = Iterables.limit(sortedQueues, limit);

                if (!(servedQueues.iterator().hasNext())) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    final ServletOutputStream outputStream = response.getOutputStream();
                    final SimpleFilterProvider filterProvider;
                    if (fields != null) {
                        log.debug("filtered output ({})", fields);
                        filterProvider = new SimpleFilterProvider()
                            .addFilter(SQSQueue.QUEUE_FILTER, SimpleBeanPropertyFilter.filterOutAllExcept(fields));
                    } else {
                        log.debug("unfiltered output");
                        filterProvider = NOOP_QUEUE_FILTER;
                    }
                    mapper.writer(filterProvider).writeValue(outputStream, Lists.newArrayList(servedQueues));
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                final ServletOutputStream outputStream = response.getOutputStream();
                outputStream.print(e.toString());
                outputStream.close();
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("I/O error handling SQS request", e);
        }
    }

    private void handleComplexDynamo(HttpServletResponse response,
                                    Map<String, String[]> params,
                                    AWSDatabase db) {
        final String query = getQuery(params);
        final String sort = getSort(params);
        final int limit = getLimit(params);
        final Set<String> fields = getFields(params);

        response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        try {
            try {
                final Collection<DynamoTable> queriedTables = listTablesFromQueryExpression(query, db);
                final Collection<DynamoTable> sortedTables = sortWithExpression(queriedTables, sort);
                final Iterable<DynamoTable> servedTables = Iterables.limit(sortedTables, limit);

                if (!(servedTables.iterator().hasNext())) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    final ServletOutputStream outputStream = response.getOutputStream();
                    final SimpleFilterProvider filterProvider;
                    if (fields != null) {
                        log.debug("filtered output ({})", fields);
                        filterProvider = new SimpleFilterProvider()
                            .addFilter(DynamoTable.TABLE_FILTER, SimpleBeanPropertyFilter.filterOutAllExcept(fields));
                    } else {
                        log.debug("unfiltered output");
                        filterProvider = NOOP_TABLE_FILTER;
                    }
                    mapper.writer(filterProvider).writeValue(outputStream, Lists.newArrayList(servedTables));
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                final ServletOutputStream outputStream = response.getOutputStream();
                outputStream.print(e.toString());
                outputStream.close();
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("I/O error handling DynamoDB request", e);
        }
    }

    private void handleComplexEC2(HttpServletResponse response,
                                  Map<String, String[]> params,
                                  AWSDatabase db) {
        final String query = getQuery(params);
        final String sort = getSort(params);
        final int limit = getLimit(params);
        final Set<String> fields = getFields(params);

        response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        try {
            try {
                final Collection<EC2Instance> queriedInstances = listInstancesFromQueryExpression(query, db);
                final Collection<EC2Instance> sortedInstances = sortWithExpression(queriedInstances, sort);
                final Iterable<EC2Instance> servedInstances = Iterables.limit(sortedInstances, limit);

                if (!(servedInstances.iterator().hasNext())) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    final ServletOutputStream outputStream = response.getOutputStream();
                    final SimpleFilterProvider filterProvider;
                    if (fields != null) {
                        log.debug("filtered output ({})", fields);
                        filterProvider = new SimpleFilterProvider()
                                .addFilter(EC2Instance.INSTANCE_FILTER, SimpleBeanPropertyFilter.filterOutAllExcept(fields));
                    } else {
                        log.debug("unfiltered output");
                        filterProvider = NOOP_INSTANCE_FILTER;
                    }
                    mapper.writer(filterProvider).writeValue(outputStream, Lists.newArrayList(servedInstances));
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                final ServletOutputStream outputStream = response.getOutputStream();
                outputStream.print(e.toString());
                outputStream.close();
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("I/O error handling EC2 request", e);
        }
    }

    private void handleComplexRDS(HttpServletResponse response,
                                  Map<String, String[]> params,
                                  AWSDatabase db) {
        final String query = getQuery(params);
        final String sort = getSort(params);
        final int limit = getLimit(params);
        final Set<String> fields = getFields(params);

        response.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());

        try {
            try {
                final Collection<RDSInstance> queriedInstances = listDatabaseInstancesFromQueryExpression(query, db);
                final Collection<RDSInstance> sortedInstances = sortWithExpression(queriedInstances, sort);
                final Iterable<RDSInstance> servedInstances = Iterables.limit(sortedInstances, limit);

                if (!(servedInstances.iterator().hasNext())) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    final ServletOutputStream outputStream = response.getOutputStream();
                    final SimpleFilterProvider filterProvider;
                    if (fields != null) {
                        log.debug("filtered output ({})", fields);
                        filterProvider = new SimpleFilterProvider()
                                .addFilter(RDSInstance.INSTANCE_FILTER, SimpleBeanPropertyFilter.filterOutAllExcept(fields));
                    } else {
                        log.debug("unfiltered output");
                        filterProvider = NOOP_INSTANCE_FILTER;
                    }
                    mapper.writer(filterProvider).writeValue(outputStream, Lists.newArrayList(servedInstances));
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                final ServletOutputStream outputStream = response.getOutputStream();
                outputStream.print(e.toString());
                outputStream.close();
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("I/O error handling RDS request", e);
        }
    }

    Collection<DynamoTable> listTablesFromQueryExpression(final String expression, final AWSDatabase db)
        throws OgnlException {
        final Collection<DynamoTable> allTables = db.getDynamoTables().values();
        if (expression == null)
            return allTables;

        final Object compiled = Ognl.parseExpression(expression);
        final List<DynamoTable> tables = new ArrayList<>();

        for (DynamoTable table : allTables) {
            <fim_suffix>
            if (value instanceof Boolean && (Boolean) value)
                tables.add(table);
        }

        return tables;
    }

    Collection<SQSQueue> listQueuesFromQueryExpression(final String expression, final AWSDatabase db)
        throws OgnlException {
        final Collection<SQSQueue> allQueues = db.getSqsQueues().values();
        if (expression == null)
            return allQueues;

        final Object compiled = Ognl.parseExpression(expression);
        final List<SQSQueue> queues = new ArrayList<>();

        for (SQSQueue queue : allQueues) {
            final Object value = Ognl.getValue(compiled, queue);
            if (value instanceof Boolean && (Boolean) value)
                queues.add(queue);
        }

        return queues;
    }

    Collection<ElasticacheCluster> listCacheClustersFromQueryExpression(final String expression, final AWSDatabase db)
        throws OgnlException {
        final Collection<ElasticacheCluster> allClusters = db.getElasticacheClusters().values();
        if (expression == null)
            return allClusters;

        final Object compiled = Ognl.parseExpression(expression);
        final List<ElasticacheCluster> clusters = new ArrayList<>();

        for (ElasticacheCluster cluster : allClusters) {
            final Object value = Ognl.getValue(compiled, cluster);
            if (value instanceof Boolean && (Boolean) value)
                clusters.add(cluster);
        }

        return clusters;
    }

    <T> Collection<T> sortWithExpression(final Collection<T> set, final String expression)
        throws OgnlException {
        if (expression == null)
            return set;

        final Object compiled = Ognl.parseExpression(expression);

        final ArrayList<T> result = new ArrayList<>(set);
        Collections.sort(result, new Comparator<T>() {
            public int compare(T o1, T o2) {
                try {
                    final Object v1 = Ognl.getValue(compiled, o1);
                    final Object v2 = Ognl.getValue(compiled, o2);

                    if (v1 instanceof Comparable) {
                        return ((Comparable) v1).compareTo(v2);
                    }
                } catch (OgnlException e) {
                    return 0;
                }
                return 0;
            }
        });

        return result;
    }

    Collection<EC2Instance> listInstancesFromQueryExpression(final String expression, final AWSDatabase db)
            throws OgnlException {
        final Collection<EC2Instance> allInstances = db.getEc2Instances().values();
        if (expression == null)
            return allInstances;

        final Object compiled = Ognl.parseExpression(expression);
        final List<EC2Instance> instances = new ArrayList<>();

        for (EC2Instance instance : allInstances) {
            final Object value = Ognl.getValue(compiled, instance);
            if (value instanceof Boolean && (Boolean) value)
                instances.add(instance);
        }

        return instances;
    }

    Collection<RDSInstance> listDatabaseInstancesFromQueryExpression(final String expression, final AWSDatabase db)
            throws OgnlException {
        final Collection<RDSInstance> allInstances = db.getRdsInstances().values();
        if (expression == null)
            return allInstances;

        final Object compiled = Ognl.parseExpression(expression);
        final List<RDSInstance> instances = new ArrayList<>();

        for (RDSInstance instance : allInstances) {
            final Object value = Ognl.getValue(compiled, instance);
            if (value instanceof Boolean && (Boolean) value)
                instances.add(instance);
        }

        return instances;
    }

    private String getQuery(Map<String, String[]> params) {
        final String[] qs = params.get("q");
        if (qs != null)
            return qs[0];
        final String[] queries = params.get("query");
        if (queries != null)
            return queries[0];
        return null;
    }

    private String getSort(Map<String, String[]> params) {
        final String[] ss = params.get("s");
        if (ss != null)
            return ss[0];
        final String[] sorts = params.get("sort");
        if (sorts != null)
            return sorts[0];
        return null;
    }

    private int getLimit(Map<String, String[]> params) {
        final String[] ls = params.get("l");
        if (ls != null)
            return Integer.parseInt(ls[0]);
        final String[] limits = params.get("limit");
        if (limits != null)
            return Integer.parseInt(limits[0]);
        return Integer.MAX_VALUE;
    }

    private Set<String> getFields(Map<String, String[]> params) {
        final String[] fs = params.get("f");
        if (fs != null)
            return Sets.newHashSet(Splitter.on(',').split(fs[0]));
        final String[] fields = params.get("fields");
        if (fields != null)
            return Sets.newHashSet(Splitter.on(',').split(fields[0]));
        return null;
    }
}
<fim_middle>