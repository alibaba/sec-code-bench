<filename>src/main/java/alchemystar/lancelot/route/visitor/LancelotOutputVisitor.java<fim_prefix>

        /*
         * Copyright (C) 2016 alchemystar, Inc. All Rights Reserved.
         */
        package alchemystar.lancelot.route.visitor;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLSetQuantifier;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;

import alchemystar.lancelot.loader.TableRuleConfig;
import alchemystar.lancelot.route.util.VelocityUtil;

/**
 * LancelotOutputVisitor
 * 这边可以加上路由策略,用策略模式
 *
 * @Author lizhuyang
 */
public class LancelotOutputVisitor extends MySqlOutputVisitor {

    private final Map<String, TableRuleConfig> tableRules;
    private final SchemaStatVisitor visitor;

    public LancelotOutputVisitor(Appendable appender, Map<String, TableRuleConfig> tableRules,
                                 SchemaStatVisitor visitor) {
        super(appender);
        this.tableRules = tableRules;
        this.visitor = visitor;
    }


    @Override
    public boolean visit(MySqlSelectQueryBlock x) {
        if (x.getOrderBy() != null) {
            x.getOrderBy().setParent(x);
        }

        print0(ucase ? "SELECT " : "select ");

        for (int i = 0, size = x.getHintsSize(); i < size; ++i) {
            SQLCommentHint hint = x.getHints().get(i);
            hint.accept(this);
            print(' ');
        }

        if (SQLSetQuantifier.ALL == x.getDistionOption()) {
            print0(ucase ? "ALL " : "all ");
        } else if (SQLSetQuantifier.DISTINCT == x.getDistionOption()) {
            print0(ucase ? "DISTINCT " : "distinct ");
        } else if (SQLSetQuantifier.DISTINCTROW == x.getDistionOption()) {
            print0(ucase ? "DISTINCTROW " : "distinctrow ");
        }

        if (x.isHignPriority()) {
            print0(ucase ? "HIGH_PRIORITY " : "high_priority ");
        }

        if (x.isStraightJoin()) {
            print0(ucase ? "STRAIGHT_JOIN " : "straight_join ");
        }

        if (x.isSmallResult()) {
            print0(ucase ? "SQL_SMALL_RESULT " : "sql_small_result ");
        }

        if (x.isBigResult()) {
            print0(ucase ? "SQL_BIG_RESULT " : "sql_big_result ");
        }

        if (x.isBufferResult()) {
            print0(ucase ? "SQL_BUFFER_RESULT " : "sql_buffer_result ");
        }

        if (x.getCache() != null) {
            if (x.getCache().booleanValue()) {
                print0(ucase ? "SQL_CACHE " : "sql_cache ");
            } else {
                print0(ucase ? "SQL_NO_CACHE " : "sql_no_cache ");
            }
        }

        if (x.isCalcFoundRows()) {
            print0(ucase ? "SQL_CALC_FOUND_ROWS " : "sql_calc_found_rows ");
        }

        printSelectList(x.getSelectList());

        if (x.getInto() != null) {
            println();
            print0(ucase ? "INTO " : "into ");
            x.getInto().accept(this);
        }

        if (x.getFrom() != null) {
            println();
            print0(ucase ? "FROM " : "from ");
            // modify lizhuyang
            renderTB(x);
            x.getFrom().accept(this);
        }

        if (x.getWhere() != null) {
            println();
            print0(ucase ? "WHERE " : "where ");
            x.getWhere().setParent(x);
            x.getWhere().accept(this);
        }

        if (x.getGroupBy() != null) {
            println();
            x.getGroupBy().accept(this);
        }

        if (x.getOrderBy() != null) {
            println();
            x.getOrderBy().accept(this);
        }

        if (x.getLimit() != null) {
            println();
            x.getLimit().accept(this);
        }

        if (x.getProcedureName() != null) {
            print0(ucase ? " PROCEDURE " : " procedure ");
            x.getProcedureName().accept(this);
            if (!x.getProcedureArgumentList().isEmpty()) {
                print('(');
                printAndAccept(x.getProcedureArgumentList(), ", ");
                print(')');
            }
        }

        if (x.isForUpdate()) {
            println();
            print0(ucase ? "FOR UPDATE" : "for update");
        }

        if (x.isLockInShareMode()) {
            println();
            print0(ucase ? "LOCK IN SHARE MODE" : "lock in share mode");
        }

        return false;
    }

    private void renderTB(MySqlSelectQueryBlock x) {
        if (x.getFrom() instanceof SQLExprTableSource) {
            SQLExprTableSource from = (SQLExprTableSource) x.getFrom();
            if (from.getExpr() instanceof SQLIdentifierExpr) {
                SQLIdentifierExpr expr = (SQLIdentifierExpr) from.getExpr();
                if (tableRules.containsKey(expr.getName())) {
                    expr.setName(renderTB(expr.getName(), tableRules.get(expr.getName())));
                    // expr.setName(expr.getName());
                }
            }
        }
    }

    private String renderTB(String tableName, TableRuleConfig config) {
        String hitColumn = null;
        String hitColumnValue = null;
        for (TableStat.Condition condition : visitor.getConditions()) {
            if (config.contains(condition.getColumn().getName())) {
                if (condition.getOperator().equals("=")) {
                    hitColumn = condition.getColumn().getName();
                    hitColumnValue = condition.getValues().get(0).toString();
                    break;
                }

            }
        }
        // 没有命中,则返回原值
        if (hitColumn == null) {
            return tableName;
        }
        String tbTbl = config.getTbRender();
        String dbTpl = config.getDbRender();
        Writer writer = new StringWriter();
        try {
            VelocityContext context = VelocityUtil.getContext();
            context.put(hitColumn, hitColumnValue);
<fim_suffix>
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
<fim_middle>