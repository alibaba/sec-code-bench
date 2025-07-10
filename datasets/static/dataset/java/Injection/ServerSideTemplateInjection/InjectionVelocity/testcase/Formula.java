<filename>src/main/java/org/olap/server/database/physical/Formula.java<fim_prefix>

        package org.olap.server.database.physical;

import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.olap4j.OlapException;

import com.healthmarketscience.sqlbuilder.CustomExpression;
import com.healthmarketscience.sqlbuilder.Expression;

public class Formula {

    private String formula;
    private static VelocityEngine ve;

    static {
        Properties config = new Properties();
        config.put( VelocityEngine.RUNTIME_REFERENCES_STRICT, "true");
        config.put( VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.Log4JLogChute");
        ve = new VelocityEngine();
        ve.init(config);
    }

    public Formula(String formula) {
        this.formula = formula;
    }

    public Expression render(AggregateTable table) throws OlapException {

        VelocityContext context = new VelocityContext();
        for(TableMeasure m : table.measures){
            if(m.getExpression()!=null)
                context.put(m.measure.replace(" ", "_"), m.getExpression().toString());
        }

        StringWriter w = new StringWriter();

<fim_suffix>

        return new CustomExpression(w.toString());
    }

}
<fim_middle>