<filename>src/main/java/com/jte/sync2any/extract/impl/MysqlMetaExtractImpl.java<fim_prefix>

        package com.jte.sync2any.extract.impl;

import cn.hutool.db.DbUtil;
import com.jte.sync2any.exception.ShouldNeverHappenException;
import com.jte.sync2any.extract.DbMetaExtract;
import com.jte.sync2any.model.config.Conn;
import com.jte.sync2any.model.config.SourceMysqlDb;
import com.jte.sync2any.model.mysql.ColumnMeta;
import com.jte.sync2any.model.mysql.IndexMeta;
import com.jte.sync2any.model.mysql.IndexType;
import com.jte.sync2any.model.mysql.TableMeta;
import com.jte.sync2any.util.DbUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MysqlMetaExtractImpl implements DbMetaExtract {

    public final String GET_ALL_TABLES_SQL="select table_name from information_schema.tables where table_schema=? and table_type='base table'";
    public final String GET_COUNT_SQL="select count(*) from #{table_name}"; ;

    @Resource
    @Qualifier(allSourceTemplate)
    Map<String,JdbcTemplate> allSourceTemplate;

    @Resource
    SourceMysqlDb sourceMysqlDb;

    @Override
    public TableMeta getTableMate(String dbId,String tableName) {
        JdbcTemplate jdbcTemplate=getJdbcTemplate(dbId);
        <fim_suffix>
        String sql = "SELECT * FROM " + tableName +  "LIMIT 1";
        Connection conn=null;
        try {
            conn=jdbcTemplate.getDataSource().getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            return resultSetMetaToSchema(rs.getMetaData(), conn.getMetaData());
        }
        catch (Exception e) {
            throw new ShouldNeverHappenException(String.format("Failed to fetch schema of %s", tableName), e);
        }
        finally {
            DbUtil.close(conn);
        }
    }

    @Override
    public Long getDataCount(String dbId, String tableName) {
        JdbcTemplate jdbcTemplate=getJdbcTemplate(dbId);
        String sql = new String(GET_COUNT_SQL);
        sql=sql.replace("#{table_name}",tableName);
        Long count=jdbcTemplate.queryForObject(sql,Long.class);
        return count;
    }

    @Override
    public List<String> getAllTableName(String dbId) {
        JdbcTemplate jdbcTemplate=getJdbcTemplate(dbId);
        Conn conn=DbUtils.getConnByDbId(sourceMysqlDb.getDatasources(),dbId);
        List<String> tableNameList=jdbcTemplate.queryForList(GET_ALL_TABLES_SQL,String.class,new String[]{conn.getDbName()});
        return tableNameList;
    }

    @Override
    public String getTableEngineName(String dbId, String tableName) {
        return null;
    }

    private JdbcTemplate getJdbcTemplate(String dbId)
    {
        JdbcTemplate jdbcTemplate=allSourceTemplate.get(dbId);
        if(Objects.isNull(jdbcTemplate))
        {
            throw new ShouldNeverHappenException("can not find jdbcTemplate for db id:"+dbId);
        }
        return jdbcTemplate;
    }

    private TableMeta resultSetMetaToSchema(ResultSetMetaData rsmd, DatabaseMetaData dbmd)
            throws SQLException {
        //always  for mysql
        String schemaName = rsmd.getSchemaName(1);
        String catalogName = rsmd.getCatalogName(1);
        /*
         * use ResultSetMetaData to get the pure table name
         * can avoid the problem below
         *
         * select * from account_tbl
         * select * from account_TBL
         * select * from `account_tbl`
         * select * from account.account_tbl
         */
        String tableName = rsmd.getTableName(1);

        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);
        tm.setDbName(catalogName.toLowerCase());
//        tm.setDbName();

        /*
         * here has two different type to get the data
         * make sure the table name was right
         * 1. show full columns from xxx from xxx(normal)
         * 2. select xxx from xxx where catalog_name like ? and table_name like ?(informationSchema=true)
         */

        try (ResultSet rsColumns = dbmd.getColumns(catalogName, schemaName, tableName);
             ResultSet rsIndex = dbmd.getIndexInfo(catalogName, schemaName, tableName, false, true)) {
            while (rsColumns.next()) {
                ColumnMeta col = new ColumnMeta();
                col.setTableCat(rsColumns.getString(TABLE_CAT));
                col.setTableSchemaName(rsColumns.getString(TABLE_SCHEM));
                col.setTableName(rsColumns.getString(TABLE_NAME));
                col.setColumnName(rsColumns.getString(COLUMN_NAME));
                col.setDataType(rsColumns.getInt(DATA_TYPE));
                col.setDataTypeName(rsColumns.getString(TYPE_NAME));
                col.setColumnSize(rsColumns.getInt(COLUMN_SIZE));
                col.setDecimalDigits(rsColumns.getInt(DECIMAL_DIGITS));
                col.setNumPrecRadix(rsColumns.getInt(NUM_PREC_RADIX));
                col.setNullAble(rsColumns.getInt(NULLABLE));
                col.setRemarks(rsColumns.getString(REMARKS));
                col.setColumnDef(rsColumns.getString(COLUMN_DEF));
                col.setSqlDataType(rsColumns.getInt(SQL_DATA_TYPE));
                col.setSqlDatetimeSub(rsColumns.getInt(SQL_DATETIME_SUB));
                col.setCharOctetLength(rsColumns.getInt(CHAR_OCTET_LENGTH));
                col.setOrdinalPosition(rsColumns.getInt(ORDINAL_POSITION));
                col.setIsNullAble(rsColumns.getString(IS_NULLABLE));
                col.setIsAutoincrement(rsColumns.getString(IS_AUTOINCREMENT));
                tm.getAllColumnMap().put(col.getColumnName(), col);
                tm.getAllColumnList().add(col);
            }

            while (rsIndex.next()) {
                String indexName = rsIndex.getString(INDEX_NAME);
                String colName = rsIndex.getString(COLUMN_NAME);
                ColumnMeta col = tm.getAllColumnMap().get(colName);

                if (tm.getAllIndexes().containsKey(indexName)) {
                    IndexMeta index = tm.getAllIndexes().get(indexName);
                    index.getValues().add(col);
                } else {
                    IndexMeta index = new IndexMeta();
                    index.setIndexName(indexName);
                    index.setNonUnique(rsIndex.getBoolean(NON_UNIQUE));
                    index.setIndexQualifier(rsIndex.getString(INDEX_QUALIFIER));
                    index.setIndexName(rsIndex.getString(INDEX_NAME));
                    index.setType(rsIndex.getShort(TYPE));
                    index.setOrdinalPosition(rsIndex.getShort(ORDINAL_POSITION));
                    index.setAscOrDesc(rsIndex.getString(ASC_OR_DESC));
                    index.setCardinality(rsIndex.getInt(CARDINALITY));
                    index.getValues().add(col);
                    if (PRIMARY.equalsIgnoreCase(indexName)) {
                        index.setIndextype(IndexType.PRIMARY);
                    } else if (!index.isNonUnique()) {
                        index.setIndextype(IndexType.UNIQUE);
                    } else {
                        index.setIndextype(IndexType.NORMAL);
                    }
                    tm.getAllIndexes().put(indexName, index);
                }
            }
            if (tm.getAllIndexes().isEmpty()) {
                throw new ShouldNeverHappenException("Could not found any index in the table:  "+ tableName);
            }
        }
        return tm;
    }
}

<fim_middle>