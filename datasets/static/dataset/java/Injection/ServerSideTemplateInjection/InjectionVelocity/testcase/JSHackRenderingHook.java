<filename>api/src/main/java/org/oscelot/jshack/hooks/JSHackRenderingHook.java<fim_prefix>

        /*
         * To change this template, choose Tools | Templates
         * and open the template in the editor.
         */
        package org.oscelot.jshack.hooks;

import blackboard.platform.context.Context;
import blackboard.platform.context.ContextManager;
import blackboard.platform.context.ContextManagerFactory;
import blackboard.platform.plugin.PlugInUtil;
import blackboard.servlet.renderinghook.RenderingHook;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.oscelot.jshack.JSHackManager;

//import org.oscelot.jshack.JSHackManagerFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.oscelot.jshack.BuildingBlockHelper;
import org.oscelot.jshack.model.Hack;
import org.oscelot.jshack.model.HackRenderingContext;
import org.oscelot.jshack.resources.HackResource;
import org.oscelot.jshack.service.HackManager;
import org.oscelot.jshack.service.HackManagerFactory;

/**
 * Base class for the JSHack rendering hook implementations.
 *
 * @author Wiley Fuller <wfuller@swin.edu.au>
 */
public abstract class JSHackRenderingHook implements RenderingHook {

    @Override
    public abstract String getKey();

    protected HackManager hackManager;

    protected ContextManager cm;
    private VelocityEngine ve;

    @Override
    public String getContent() {
        Hack currPkg = null;
        try {
//            hackManager = getHackManager();
            StringBuilder output = new StringBuilder();

            Context context = getContextManager().getContext();
            if (context == null) {
                return "";
            }

            HackRenderingContext hackCtx = getHackManager().getRenderingContext(this.getKey(), context);
            List<HackResource> resources = hackCtx.getResources();

            if (resources != null) {
                for (HackResource resource : resources) {
                    Hack hack = resource.getHack();
                    String hackId = hack.getIdentifier();
                    StringBuilder sb = new StringBuilder("\n<!-- START HACK : ");
                    sb.append(hackId);
                    sb.append(" -->\n");
                    sb.append(renderResource(resource, context,
                            hackCtx.getHackConfigMaps().get(hackId),
                            hackCtx.getResourceUrlMaps().get(hackId)));
                    sb.append("\n<!-- END HACK : ");
                    sb.append(hack.getIdentifier());
                    sb.append(" -->\n");

                    output.append(sb.toString());
                }
            }

            return output.toString();
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE,
                    "Exception while processing JS Hacks. Problem with Package: " +
                            (currPkg == null ? "UNKNOWN" : currPkg.getIdentifier()), ex
            );
            ex.printStackTrace();
            return "<!-- ERROR RENDERING JS HACK - See Logs for details. -->";
        }
    }

    /**
     * Gets the current HackManager instance, or if null, gets an instance from
     * the Factory.
     *
     * @return the current HackManager instance.
     */
    protected HackManager getHackManager() {
        if (hackManager == null) {
            hackManager = HackManagerFactory.getHackManager();
        }
        return hackManager;
    }


    protected String renderResource(HackResource resource,
                                    Context context,
                                    Map<String, String> config,
                                    Map<String, String> resources) throws Exception {
        if (ve == null) {
            ve = createVelocityEngine();
        }
        VelocityContext vc = new VelocityContext();
        String baseUrl = PlugInUtil.getUriStem(BuildingBlockHelper.VENDOR_ID, BuildingBlockHelper.HANDLE);
//        long lastModified = hackManager.getHackConfigFile().lastModified();
//        String resourcePath = baseUrl+"resources/"+hack.getIdentifier()+"/"+lastModified;
//        vc.put("resourcePath", resourcePath);
        vc.put("context", context);
        vc.put("config", config);
        vc.put("resources", resources);
        StringWriter sw = new StringWriter();
<fim_suffix>

        return sw.toString();
    }

    private ContextManager getContextManager() {
        if (cm == null) {
            synchronized (this) {
                if (cm == null) {
                    cm = ContextManagerFactory.getInstance();
                }
            }
        }
        return cm;
    }

    public void setContextManager(ContextManager cm) {
        this.cm = cm;
    }


    public VelocityEngine getVelocityEngine() {
        return ve;
    }

    private synchronized VelocityEngine createVelocityEngine() {
        if (ve == null) {
            ve = new VelocityEngine();
            ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
                    "org.oscelot.jshack.Slf4jLogChute" );
        }
        return ve;
    }

    public void setVelocityEngine(VelocityEngine ve) {
        this.ve = ve;
    }

}
<fim_middle>