<filename>cqrs-event-mysql/src/main/java/com/damon/cqrs/event_store/MysqlEventStore.java<fim_prefix>

package com.damon.cqrs.event_store;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.damon.cqrs.domain.AggregateRoot;
import com.damon.cqrs.domain.Event;
import com.damon.cqrs.event.AggregateEventAppendResult;
import com.damon.cqrs.event.DomainEventStream;
import com.damon.cqrs.event.EventSendingContext;
import com.damon.cqrs.exception.EventQueryException;
import com.damon.cqrs.store.IEventShardingRouting;
import com.damon.cqrs.store.IEventStore;
import com.damon.cqrs.utils.NamedThreadFactory;
import com.damon.cqrs.utils.ReflectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * mysql事件存储器
 *
 * @author xianpinglu
 */
@Slf4j
public class MysqlEventStore implements IEventStore {
    private final String EVENT_TABLE = "event_stream_";
    private final String QUERY_AGGREGATE_EVENTS = "SELECT events FROM %s WHERE aggregate_root_id = ?  and  version >= ? and version <= ? ORDER BY version asc";
    private final String QUERY_AGGREGATE_WAITING_SEND_EVENTS = "SELECT * FROM %s  WHERE ID > ? ORDER BY  id ASC  LIMIT 20000";
    private final Map<DataSource, Integer> dataSourceMap;
    private final Map<String, DataSource> dataSourceNameMap;
    private final ExecutorService eventStoreThreadService;
    private final List<DataSource> dataSources;
    private final IEventShardingRouting eventShardingRoute;

    /**
     * @param dataSourceMappings
     * @param storeThreadNumber  事件存储异步线程处理数。如果存在分库、分表数量较多，需要调整此大小。
     */
    public MysqlEventStore(final List<DataSourceMapping> dataSourceMappings, final int storeThreadNumber, final IEventShardingRouting eventShardingRoute) {
        this.dataSourceMap = new HashMap<>();
        this.dataSourceNameMap = new HashMap<>();
        this.dataSources = new ArrayList<>();
        dataSourceMappings.forEach(mapping -> {
            dataSourceMap.put(mapping.getDataSource(), mapping.getTableNumber());
            dataSourceNameMap.put(mapping.getDataSourceName(), mapping.getDataSource());
            dataSources.add(mapping.getDataSource());
        });
        this.eventStoreThreadService = Executors.newFixedThreadPool(storeThreadNumber, new NamedThreadFactory("event-store-pool"));
        this.eventShardingRoute = eventShardingRoute;
    }

    @Override
    public List<EventSendingContext> queryWaitingSendEvents(String dataSourceName, String tableName, long offsetId) {
        try {
            QueryRunner queryRunner = new QueryRunner(dataSourceNameMap.get(dataSourceName));
            List<Map<String, Object>> rows = queryRunner.query(String.format(QUERY_AGGREGATE_WAITING_SEND_EVENTS, tableName), new MapListHandler(), offsetId);
            List<EventSendingContext> sendingContexts = rows.stream().map(map -> {
                String aggregateId = (String) map.get("aggregate_root_id");
                String aggregateType = (String) map.get("aggregate_root_type_name");
                String eventJson = (String) map.get("events");
                Long id = (Long) map.get("id");
                JSONArray array = JSONArray.parseArray(eventJson);
                List<Event> events = new ArrayList<>(array.size());
                array.forEach(object -> {
                    JSONObject jsonObject = (JSONObject) object;
                    String eventType = jsonObject.getString("eventType");
                    Event event = JSONObject.parseObject(jsonObject.toString(), ReflectUtils.getClass(eventType));
                    events.add(event);
                });
                return EventSendingContext.builder().offsetId(id).aggregateId(Long.parseLong(aggregateId)).aggregateType(aggregateType).events(events).build();
            }).collect(Collectors.toList());
            return sendingContexts;
        } catch (Throwable e) {
            throw new EventQueryException(e);
        }
    }

    @Override
    public AggregateEventAppendResult store(List<DomainEventStream> domainEventStreams) {
        //浜_欢___
        HashMap<DataSource, HashMap<String, ArrayList<DomainEventStream>>> dataSourceListMap = eventSharding(domainEventStreams);
        AggregateEventAppendResult finalResult = new AggregateEventAppendResult();
        dataSourceListMap.forEach((dataSource, tableEventStreamMap) -> {
            tableEventStreamMap.forEach((tableName, eventStreams) -> {
                AggregateEventAppendResult result = CompletableFuture.supplyAsync(
                        new EventStoreSupplier(dataSource, tableName, eventStreams),
                        eventStoreThreadService
                ).join();
                result.getDulicateCommandResults().forEach(r -> finalResult.addDulicateCommandResult(r));
                result.getExceptionResults().forEach(r -> finalResult.addExceptionResult(r));
                result.getDuplicateEventResults().forEach(r -> finalResult.addDuplicateEventResult(r));
                result.getSucceedResults().forEach(r -> finalResult.addSuccedResult(r));
            });
        });
        return finalResult;
    }

    private HashMap<DataSource, HashMap<String, ArrayList<DomainEventStream>>> eventSharding(List<DomainEventStream> domainEventStreams) {
        HashMap<DataSource, HashMap<String, ArrayList<DomainEventStream>>> dataSourceListMap = new HashMap<>();
        domainEventStreams.forEach(event -> {
            Integer dataSourceIndex = eventShardingRoute.routeInstance(event.getAggregateId(), event.getAggregateType(), dataSources.size(), event.getShardingParams());
            Integer tableNumber = dataSourceMap.get(dataSources.get(dataSourceIndex));
            Integer tableIndex = eventShardingRoute.routeSharding(event.getAggregateId(), event.getAggregateType(), tableNumber, event.getShardingParams());
            String tableName = EVENT_TABLE + tableIndex;
            dataSourceListMap.computeIfAbsent(dataSources.get(dataSourceIndex), key -> new HashMap<>()).computeIfAbsent(tableName, key -> new ArrayList<>()).add(event);
        });
        return dataSourceListMap;
    }

    @Override
    public List<List<Event>> load(long aggregateId, Class<? extends AggregateRoot> aggregateClass, int startVersion, int endVersion, Map<String, Object> shardingParams) {
        try {
            Integer dataSourceIndex = eventShardingRoute.routeInstance(aggregateId, aggregateClass.getTypeName(), dataSources.size(), shardingParams);
            QueryRunner queryRunner = new QueryRunner(dataSources.get(dataSourceIndex));
            Integer tableNumber = dataSourceMap.get(dataSources.get(dataSourceIndex));
            Integer tableIndex = eventShardingRoute.routeSharding(aggregateId, aggregateClass.getTypeName(), tableNumber, shardingParams);
            String tableName = EVENT_TABLE + tableIndex;
            List<Map<String, Object>> rows = queryRunner.query(String.format(QUERY_AGGREGATE_EVENTS, tableName), new MapListHandler(), aggregateId, startVersion, endVersion);
            List<List<Event>> events = rows.stream().map(map -> {
                String eventJson = (String) map.get("events");
                JSONArray array = JSONArray.parseArray(eventJson);
                List<Event> es = new ArrayList<>(array.size());
                array.forEach(object -> {
                    JSONObject jsonObject = (JSONObject) object;
                    String eventType = jsonObject.getString("eventType");
                    <fim_suffix>
                es.add(event);
                });
                return es;
            }).collect(Collectors.toList());
            return events;
        } catch (Throwable e) {
            throw new EventQueryException(e);
        }

    }

}
<fim_middle>