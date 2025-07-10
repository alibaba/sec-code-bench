<filename>vickze-generator/src/main/java/io/vickze/generator/service/impl/GeneratorServiceImpl.java<fim_prefix>

package io.vickze.generator.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.vickze.common.domain.RPage;
import io.vickze.common.enums.DBOrder;
import io.vickze.generator.constant.ConfigConstant;
import io.vickze.generator.constant.TemplateConstant;
import io.vickze.generator.domain.DO.ColumnDO;
import io.vickze.generator.domain.DO.ConfigDO;
import io.vickze.generator.domain.DO.TableDO;
import io.vickze.generator.domain.DO.TemplateDO;
import io.vickze.generator.domain.DTO.ColumnDTO;
import io.vickze.generator.domain.DTO.GeneratorCodeDTO;
import io.vickze.generator.domain.DTO.GeneratorQueryDTO;
import io.vickze.generator.domain.DTO.TableDTO;
import io.vickze.generator.mapper.ColumnMapper;
import io.vickze.generator.mapper.TableMapper;
import io.vickze.generator.service.ConfigService;
import io.vickze.generator.service.GeneratorService;
import io.vickze.generator.service.TemplateService;
import io.vickze.generator.util.GeneratorUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@DS("generator")
public class GeneratorServiceImpl implements GeneratorService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TableMapper tableMapper;

    @Autowired
    private ColumnMapper columnMapper;

    @Override
    public Pair<List<TableDO>, Long> tableList(GeneratorQueryDTO query) {
        IPage<TableDO> page = new RPage<>(query.getOffset(), query.getLimit());
        QueryWrapper<TableDO> wrapper = new QueryWrapper<>();
        if (query.getTableName() != null) {
            wrapper = wrapper.like("table_name", query.getTableName());
        }
        if (query.getField() != null) {
            wrapper = wrapper.orderBy(true, DBOrder.ASC.name().equalsIgnoreCase(query.getOrder()),
                    com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline(query.getField()));
        }
        wrapper.apply("table_schema = (select database())");

        tableMapper.selectPage(page, wrapper);
        return Pair.of(page.getRecords(), page.getTotal());
    }

    @Override
    public byte[] generatorCode(GeneratorCodeDTO generatorCode) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        ConfigDO configDO = configService.get(generatorCode.getConfigId());
        Collection<TemplateDO> templateDOS = templateService.listByIds(generatorCode.getTemplateIds());

        Map<String, Object> config = new HashMap<>();

        if (configDO != null) {
            if (configDO.getType() == ConfigConstant.YAML) {
                Yaml yaml = new Yaml();
                config = yaml.load(configDO.getContent());
            }
            if (configDO.getType() == ConfigConstant.PROPERTIES) {
                PropertiesConfiguration propertiesConfiguration = new PropertiesConfiguration();
                try {
                    propertiesConfiguration.load(new StringReader(configDO.getContent()));
                } catch (ConfigurationException e) {
                    throw new RuntimeException(e);
                }
                Iterator<String> iterator = propertiesConfiguration.getKeys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    config.put(key, propertiesConfiguration.getString(key));
                }
            }
        }

        for (String tableName : generatorCode.getTableNames()) {

            QueryWrapper<TableDO> tableDOQueryWrapper = new QueryWrapper<>();
            tableDOQueryWrapper.eq("table_name", tableName);
            tableDOQueryWrapper.apply("table_schema = (select database())");
            //查询表信息
            TableDO tableDO = tableMapper.selectOne(tableDOQueryWrapper);

            QueryWrapper<ColumnDO> columnDOQueryWrapper = new QueryWrapper<>();
            columnDOQueryWrapper.eq("table_name", tableName);
            columnDOQueryWrapper.apply("table_schema = (select database())");
            columnDOQueryWrapper.orderByAsc("ordinal_position");
            //查询列信息
            List<ColumnDO> columnDOList = columnMapper.selectList(columnDOQueryWrapper);


            // 表信息
            TableDTO tableDTO = new TableDTO();
            tableDTO.setName(tableDO.getTableName());
            tableDTO.setComments(tableDO.getTableComment());
            // 表名转换成Java类名
            String className = GeneratorUtil.tableToJava(tableDTO.getName(), String.valueOf(config.get("tablePrefix")));
            tableDTO.setClassNameUpperCamel(className);
            tableDTO.setClassNameLowerCamel(StringUtils.uncapitalize(className));

            // 列信息
            List<ColumnDTO> columns = new ArrayList<>();
            for (ColumnDO columnDO : columnDOList) {
                ColumnDTO column = new ColumnDTO();
                column.setName(columnDO.getColumnName());
                column.setDataType(columnDO.getDataType());
                column.setComment(columnDO.getColumnComment());
                column.setExtra(columnDO.getExtra());
                column.setIsNullable(columnDO.getIsNullable());

                // 列名转换成Java属性名
                String attrName = GeneratorUtil.columnToJava(column.getName());
                column.setAttrNameUpperCamel(attrName);
                column.setAttrNameLowerCamel(StringUtils.uncapitalize(attrName));

                // 列的数据类型，转换成Java类型
                String attrType = String.valueOf(config.get(column.getDataType()));
                if (attrType == null || "null".equals(attrType)) {
                    attrType = "unknowType";
                }
                column.setAttrType(attrType);

                // 是否主键
                if ("PRI".equalsIgnoreCase(columnDO.getColumnKey()) && tableDTO.getPk() == null) {
                    tableDTO.setPk(column);
                }

                column.setCharacterMaximumLength(columnDO.getCharacterMaximumLength());
                columns.add(column);
            }
            tableDTO.setColumns(columns);

            // 没主键，则第一个字段为主键
            if (tableDTO.getPk() == null) {
                tableDTO.setPk(tableDTO.getColumns().get(0));
            }


            Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);

            Velocity.init();

            // 封装模板数据
            config.put("tableName", tableDTO.getName());
            config.put("tableComment", tableDTO.getComments());
            config.put("pk", tableDTO.getPk());
            config.put("classNameUpperCamel", tableDTO.getClassNameUpperCamel());
            config.put("classNameLowerCamel", tableDTO.getClassNameLowerCamel());
            config.put("pathName", StringUtils.replace(tableDTO.getName(), "_", "/"));
            config.put("columns", tableDTO.getColumns());
            config.put("now", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            VelocityContext velocityContext = new VelocityContext();
            Iterator<String> iterator = config.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                velocityContext.put(key, config.get(key));
            }

            for (TemplateDO template : templateDOS) {
                try {
                    // 输出流
                    StringWriter writer = new StringWriter();

                    if (TemplateConstant.VELOCITY.equals(template.getType())) {
                        // 转换输出
                        Velocity.evaluate(velocityContext, writer, "", template.getContent()); // 关键方法
                        // 添加到zip
                        zip.putNextEntry(new ZipEntry(GeneratorUtil.getFileName(velocityContext, template.getFileName(), tableDTO.getClassNameUpperCamel(), tableDTO.getClassNameLowerCamel(), String.valueOf(config.get("package")))));
                        IOUtils.write(writer.toString(), zip, "UTF-8");
                        IOUtils.closeQuietly(writer);
                        zip.closeEntry();
                    }

                    if (TemplateConstant.FREEMARKER.equals(template.getType())) {
                        <fim_suffix>

                        // 添加到zip
                        zip.putNextEntry(new ZipEntry(GeneratorUtil.getFileName(configuration, template.getFileName(), config)));
                        IOUtils.write(writer.toString(), zip, "UTF-8");
                        IOUtils.closeQuietly(writer);
                        zip.closeEntry();
                    }
                } catch (Exception e) {
                    throw new RuntimeException("渲染模板失败，表名：" + tableDTO.getName(), e);
                }
            }
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }
}
<fim_middle>