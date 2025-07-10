<filename>api/src/main/java/edu/cornell/mannlib/vitro/webapp/web/widgets/Widget.java<fim_prefix>

/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.web.widgets;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import edu.cornell.mannlib.vitro.webapp.web.templatemodels.Tags;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import freemarker.core.Environment;
import freemarker.core.Macro;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

public abstract class Widget {

    private static final Log log = LogFactory.getLog(Widget.class);

    /* Widget implementations don't get any state when they get constructed so that they
     * can be reused. */
    public Widget() { }

    public String doAssets(Environment env, Map params) {
        String widgetName = params.get("name").toString(); //getWidgetName();
        Macro assetsMacro = getMacroFromTemplate(getAssetsMacroName(), widgetName, env);

        // Allow there to be no assets macro in the template
        if (assetsMacro == null) {
            return "";
        }
        TemplateHashModel dataModel = env.getDataModel();
        Map<String, Object> map = new HashMap<String, Object>();

        try {
            // Once we remove portals, we can define these as Configuration shared variables. Then the
            // templates will automatically get these and we don't have to add them to the data model.
            map.put("stylesheets", dataModel.get("stylesheets"));
            map.put("scripts", dataModel.get("scripts"));
            map.put("headScripts", dataModel.get("headScripts"));
            map.put("metaTags", new Tags().wrap());
            map.put("urls", dataModel.get("urls"));
        } catch (TemplateModelException e) {
            log.error("Error getting asset values from data model.");
        }

        return processMacroToString(env, widgetName, assetsMacro, map);
    }

    public String doMarkup(Environment env, Map params) {
        HttpServletRequest request = (HttpServletRequest) env.getCustomAttribute("request");
        ServletContext context = request.getSession().getServletContext();

        WidgetTemplateValues values = null;

        try {
            values = process(env, params, request, context);
        } catch (Exception e) {
            log.error(e, e);
        }

        // The widget process() method may determine that nothing should display for the widget:
        // for example, the login widget doesn't display if the user is already logged in. This also
        // applies if process() threw an error.
        if (values == null) {
            return "";
        }
        String widgetName = params.get("name").toString(); // getWidgetName();
        return processMacroToString(env, widgetName, values);
    }

    // Default  template name. Can be overridden by subclasses.
    protected String getTemplateName(String widgetName) {
        return "widget-" + widgetName + ".ftl";
    }

    // Default assets macro name. Can be overridden by subclasses.
    protected String getAssetsMacroName() {
        return "assets";
    }

    // Default markup macro name. Can be overridden by subclasses, or
    // subclass process() method can select from various markup macros
    // based on widget state. For example, the login widget markup macro will
    // differ depending on login processing state.
    protected String getMarkupMacroName() {
        return "markup";
    }

//    private String getWidgetName() {
//        String name = this.getClass().getName();
//        name= name.replaceAll(".*\\.", "");
//        name = name.replaceAll("Widget$", "");
//        name = name.substring(0, 1).toLowerCase() + name.substring(1);
//        return name;
//    }

    protected abstract WidgetTemplateValues process(Environment env, Map params,
            HttpServletRequest request, ServletContext context) throws Exception;

    private String processMacroToString(Environment env, String widgetName, Macro macro, Map<String, Object> map) {
        StringWriter out = new StringWriter();

        try {
            String templateString = getTemplateString(macro);
            // NB Using this method of creating a template from a string does not allow the widget template to import
            // other templates (but it can include other templates). We'd need to use a StringTemplateLoader
            // in the config instead. See StringTemplateLoader API doc.
            // The problem is that the StringTemplateLoader has to be added to the config's MultiTemplateLoader.
            // Then to support multi-threading, we can't just add the widget here to the StringTemplateLoader with
            // the same key, e.g., "widgetTemplate", since one putTemplate() call will clobber a previous one.
            // We need to give each widget macro template a unique key in the StringTemplateLoader, and check
            // if it's already there or else add it. Leave this for later.
            <fim_suffix>

            template.process(map, out);
        } catch (Exception e) {
            log.error("Could not process widget " + widgetName, e);
        }
        String output = out.toString();
        log.debug("Macro output: " + output);
        return output;
    } 

    private String getTemplateString(Macro macro) throws TemplateModelException {
        TemplateSequenceModel childNodes = macro.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.size(); i++) {
            sb.append(childNodes.get(i));
        }
        String template = sb.toString();
        return template;
    }

    private String processMacroToString(Environment env, String widgetName, String macroName, Map<String, Object> map) {
        Macro macro = getMacroFromTemplate(macroName, widgetName, env);
        return processMacroToString(env, widgetName, macro, map);
    }

    private String processMacroToString(Environment env, String widgetName, WidgetTemplateValues values) {
        return processMacroToString(env, widgetName, values.getMacroName(), values.getMap());
    }

    private Macro getMacroFromTemplate(String macroName, String widgetName, Environment env) {
        String templateName = getTemplateName(widgetName);
        Template template = null;
        Macro macro = null;
        try {
            template = env.getConfiguration().getTemplate(templateName);
            macro = (Macro)template.getMacros().get(macroName);
        } catch (IOException e) {
            log.error("Cannot get template " + templateName);
        }
        return macro;
    }

    protected static class WidgetTemplateValues {
        private final String macroName;
        private final Map<String, Object> map;

        public WidgetTemplateValues(String macroName) {
            this.macroName = macroName;
            this.map = new HashMap<String, Object>();
        }

        public WidgetTemplateValues(String macroName, Map<String, Object> map) {
            this.macroName = macroName;
            this.map = map;
        }

        public WidgetTemplateValues put(String key, Object value) {
            this.map.put(key, value);
            return this;
        }

        public Map<String, Object> getMap() {
            return Collections.unmodifiableMap(this.map);
        }

        public String getMacroName() {
            return this.macroName;
        }
    }

    protected class WidgetProcessingException extends Exception {
        WidgetProcessingException(String message) {
            super(message);
        }
    }

}
<fim_middle>