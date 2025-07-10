<filename>src/main/java/com/norconex/commons/lang/config/ConfigurationLoader.java<fim_prefix>

        /* Copyright 2010-2023 Norconex Inc.
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */
        package com.norconex.commons.lang.config;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.xml.sax.ErrorHandler;

import com.norconex.commons.lang.bean.BeanMapper;
import com.norconex.commons.lang.bean.BeanMapper.Format;
import com.norconex.commons.lang.xml.Xml;

import jakarta.validation.ConstraintViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

/**
 * <p>Configuration file parser using Velocity template engine
 * which supports variables, parse/include directives, and more).
 * </p>
 * <h2>Variables</h2>
 * <p>
 * Variables can be defined in a few different ways (in order of precedence):
 * system properties, environment variable, or variable files. In a
 * configuration file, variables are referenced surrounded by curly braces
 * and prefixed with a dollar sign.  Default values can be specified by
 * following the variable name with a vertical bar character and the value.
 * Examples:
 * </p>
 * <ul>
 *   <li>
 *     <code>${pageTitle}</code> &rarr; Prints the value of a variable named
 *     "pageTitle" or nothing if no title variable is found.
 *   </li>
 *   <li>
 *     <code>${pageTitle|'Hello world!'}</code> &rarr; Prints the value of a
 *     variable named "pageTitle", or "Hello world!" if no title variable is
 *     found.
 *   </li>
 * </ul>
 * <h2>System Properties</h2>
 * <p>
 * System properties are typically passed to the JVM at launch time with
 * the <code>-D</code> argument.
 * Variables defined as system properties take precedence over variables
 * of the same name defined any other way.  Character case, as well
 * as non-alphanumeric characters have no importance in the variable
 * resolution. For instance, all of the following are equivalent:
 * </p>
 * <ul>
 *   <li><code>-DpageTitle</code></li>
 *   <li><code>-Dpagetitle</code></li>
 *   <li><code>-DPAGE_TITLE</code></li>
 * </ul>
 *
 * <h2>Environment variables</h2>
 * <p>
 * Environment variables are typically set at a user account level, or
 * operating system level. Environment variables take precedence over
 * variable files, but not over system properties.
 * Like system properties, character case, as well
 * as non-alphanumeric characters have no importance in the variable
 * resolution. For instance, all of the following are equivalent:
 * </p>
 * <ul>
 *   <li><code>pageTitle</code></li>
 *   <li><code>pagetitle</code></li>
 *   <li><code>PAGE_TITLE</code></li>
 * </ul>
 *
 * <h2>Implicit variable files</h2>
 * <p>
 * Configuration templates, whether the main template or any template
 * included using the <code>#parse</code> directive, can have variable files
 * attached, for which each key would become a variable in the Velocity
 * context.  A variable file must be of the same name as the template file,
 * with one of two possible extensions:
 * <code>.variables</code> or <code>.properties</code>. By respecting this
 * naming condition, the variable files do not have to be explicitly specified
 * as argument.
 * </p>
 * <p>
 * A <code>.variables</code> file must have keys and values separated by an
 * equal sign, one variable per line.  The key and value strings are taken
 * literally, after trimming leading and trailing spaces.
 * </p>
 * <p>
 * A <code>.properties</code> file stores key/value in the way the Java
 * programming language expects it for any <code>.properties</code> file.
 * It is essentially the same, but has more options (e.g. multi-line support)
 * and gotchas (e.g. must escape certain characters). Please
 * refer to the corresponding
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html#load-java.io.Reader-">
 * Java API documentation</a> for exact syntax and parsing logic.
 * </p>
 * <p>
 * When both <code>.variables</code> and <code>.properties</code> exist
 * for a template, the <code>.properties</code> file variables take
 * precedence.
 * </p>
 *
 * <h2>Explicit variable files</h2>
 * <p>
 * Any <code>.variables</code> or <code>.properties</code> file
 * can also be specified using the
 * {@link ConfigurationLoaderBuilder#setVariablesFile(Path)} method.
 * <b>Since 3.0.0</b>, any variable files without the <code>.properties</code>
 * extension as if it was <code>.variables</code>. The syntax for both file
 * types is the same as described under <em>Implicit variable files</em> above.
 * </p>
 *
 * <h2>Configuration fragments</h2>
 * <p>
 * To include configuration fragments and favor reuse, use the
 * <code>#include("myfile.cg")</code> or <code>#parse("myfile.cg")</code>
 * directives.  An include directive will include the referenced file
 * as-is, without interpretation.  A parse directive will treat the included
 * file as a Velocity file and will interpret it (along with its variable
 * file if any exists -- see above).
 * </p>
 * <p>
 * The included/parsed files are relative to the parent template, or, can be
 * absolute paths on the host where the configuration loader is executed.
 * Example (both Windows and UNIX path styles are supported):
 * </p>
 * <p>
 * <i>Sample directory structure:</i>
 * </p>
 * <pre>
 * c:\sample\
 *     myapp\
 *         runme.jar
 *         configs\
 *              myconfig.cfg
 *              myconfig.properties
 *     shared\
 *         sharedconfig.cfg
 *         sharedconfig.variables
 * </pre>
 * <p>
 * <i>Configuration file myconfig.cfg:</i>
 * </p>
 * <pre>
 * &lt;myconfig&gt;
 *    &lt;host&gt;$host&lt;/host&gt;
 *    &lt;port&gt;$port&lt;/port&gt;
 *    #parse("../../shared/sharedconfig.cfg")
 * &lt;/myconfig&gt;
 * </pre>
 * <p>
 * <i>Configuration loading:</i>
 * </p>
 * <pre>
 * XML xml = new ConfigurationLoader().loadXML(
 *         Path.get("/path/to/myconfig.cfg"));
 * </pre>
 * <p>
 * <i>Explanation:</i>
 * </p>
 * <p>
 * When loading myconfig.cfg, the variables defined in myconfig.properties
 * are automatically loaded and will replace the $host and $port variables.
 * The myconfig.cfg file is also parsing a shared configuration file:
 * sharedconfig.cfg.  That file will be parsed and inserted,
 * with its variables defined in sharedconfig.variables automatically
 * loaded and resolved.
 * </p>
 * <p>
 * Other Velocity directives are supported
 * (if-else statements, foreach loops, macros,
 * etc.).  Refer to
 * <a href="https://velocity.apache.org/engine/2.0/user-guide.html">
 * Velocity User Guide</a> for complete syntax and template documentation.
 * </p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public final class ConfigurationLoader {


    /** Optional Velocity context. */
    private VelocityContext defaultContext;
    /** Optional custom bean mapper when loading into object. */
    @Default
    @NonNull
    private BeanMapper beanMapper = BeanMapper.DEFAULT;
    /** File holding variables. See class documentation. */
    private Path variablesFile;

    /**
     * @deprecated Use {@link ConfigurationLoader#builder()} instead.
     */
    @Deprecated(since = "3.0.0")
    public ConfigurationLoader() {
    }

    /**
     * Sets a variables file. See class documentation for details.
     * @param variablesFile variables file
     * @return this instance
     * @since 2.0.0
     * @deprecated Use {@link ConfigurationLoaderBuilder#variablesFile(Path)}
     * instead.
     */
    @Deprecated(since = "3.0.0")
    public ConfigurationLoader setVariablesFile(Path variablesFile) {
        this.variablesFile = variablesFile;
        return this;
    }

    /**
     * Loads a configuration file as a string, performing variable
     * interpolation and handling any other Velocity directives.
     * @param configFile configuration file
     * @return configuration as string
     * @since 3.0.0, renamed from <code>loadString</code>
     */
    public String toString(Path configFile) {
        if (configFile == null) {
            throw new ConfigurationException(
                    "No configuration file specified.");
        }
        if (!configFile.toFile().exists()) {
            return null;
        }

        var context = new VelocityContext(defaultContext);

        // Load from explicitly referenced properties
        VariablesFileResolver.resolve(variablesFile).forEach(context::put);

        // Load from properties matching config file name
        var file = configFile.toAbsolutePath().toString();
        var fullpath = FilenameUtils.getFullPath(file);
        var baseName = FilenameUtils.getBaseName(file);

        VariablesFileResolver.resolve(fullpath, baseName).forEach(context::put);

        var sw = new StringWriter();
        try (Reader reader = Files.newBufferedReader(configFile)) {
<fim_suffix>
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load config file as a string: " + file, e);
        }
        return sw.toString();
    }

    /**
     * Loads an XML configuration file into an {@link Xml} object, performing
     * variable interpolation and handling any other Velocity directives.
     * @param configFile XML configuration file
     * @return XML
     * @since 3.0.0 renamed from <code>loadXML(Path)</code>
     */
    public Xml toXml(Path configFile) {
        return toXml(configFile, null);
    }

    /**
     * Loads an XML configuration file into an {@link Xml} object, performing
     * variable interpolation and handling any other Velocity directives.
     * @param configFile XML configuration file
     * @param errorHandler XML error handler
     * @return XML
     * @since 3.0.0 renamed from <code>loadXML(Path, ErrorHandler)</code>
     */
    public Xml toXml(
            Path configFile, ErrorHandler errorHandler) {
        if (configFile == null || !configFile.toFile().exists()) {
            return null;
        }
        try {
            var xml = toString(configFile);
            // clean-up extra duplicate declaration tags due to template
            // includes/imports that could break parsing.
            // Keep first <?xml... tag only, and delete all <!DOCTYPE...
            // as they are not necessary to parse configs.
            xml = Pattern.compile("((?!^)<\\?xml.*?\\?>|<\\!DOCTYPE[^>]*>)")
                    .matcher(xml).replaceAll("");
            return Xml.of(xml).setErrorHandler(errorHandler).create();
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load configuration file: \"" + configFile + "\". "
                            + "Probably a misconfiguration or the configuration XML "
                            + "is not well-formed.",
                    e);
        }
    }

    /**
     * Loads an XML, JSON, or Yaml configuration file and populates a new
     * object represented by the given class. Performs variable interpolation
     * and handling of any other Velocity directives.
     * Validation error will throw a {@link ConstraintViolationException}.
     * To disable validation specify a custom {@link BeanMapper}
     * and set <code>skipValidation</code> to <code>true</code> on the mapper
     * builder.
     * The file format is dictated by the file
     * extension (XML is assumed if the file has no extension).
     * @param configFile configuration file
     * @param type class of the object to create and populate
     * @param <T> type of returned object
     * @return new object
     * @since 3.0.0
     */
    public <T> T toObject(@NonNull Path configFile, Class<T> type) {
        return beanMapper.read(
                type,
                new StringReader(toString(configFile)),
                Format.fromPath(configFile, Format.XML));
    }

    /**
     * Loads an XML, JSON, or Yaml configuration file and populates the
     * supplied object. Performs variable interpolation
     * and handling of any other Velocity directives.
     * Validation error will throw a {@link ConstraintViolationException}.
     * To disable validation specify a custom {@link BeanMapper}
     * and set <code>skipValidation</code> to <code>true</code> on the mapper
     * builder.
     * The file format is dictated by the file
     * extension (XML is assumed if the file has no extension).
     * Loads an XML configuration file and populates a given object.
     * @param configFile XML configuration file
     * @param object object to populate
     * @since 3.0.0
     */
    public void toObject(@NonNull Path configFile, @NonNull Object object) {
        beanMapper.read(
                object,
                new StringReader(toString(configFile)),
                Format.fromPath(configFile, Format.XML));
    }

    /**
     * Loads an XML configuration file.
     * @param configFile XML configuration file
     * @return XML
     * @since 2.0.0
     * @deprecated Use {@link #toXml(Path)} instead.
     */
    @Deprecated(since = "3.0.0")
    public Xml loadXML(Path configFile) {
        return loadXML(configFile, null);
    }

    /**
     * Loads an XML configuration file.
     * @param configFile XML configuration file
     * @param errorHandler XML error handler
     * @return XML
     * @since 2.0.0
     * @deprecated Use {@link #toXml(Path, ErrorHandler)} instead.
     */
    @Deprecated(since = "3.0.0")
    public Xml loadXML(
            Path configFile, ErrorHandler errorHandler) {
        if (configFile == null || !configFile.toFile().exists()) {
            return null;
        }
        try {
            var xml = loadString(configFile);
            // clean-up extra duplicate declaration tags due to template
            // includes/imports that could break parsing.
            // Keep first <?xml... tag only, and delete all <!DOCTYPE...
            // as they are not necessary to parse configs.
            xml = Pattern.compile("((?!^)<\\?xml.*?\\?>|<\\!DOCTYPE[^>]*>)")
                    .matcher(xml).replaceAll("");
            return Xml.of(xml).setErrorHandler(errorHandler).create();
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Cannot load configuration file: \"" + configFile + "\". "
                            + "Probably a misconfiguration or the configuration XML "
                            + "is not well-formed.",
                    e);
        }
    }

    /**
     * Loads an XML configuration file and populates a new object
     * represented by the given "class" attribute found on XML root element.
     * @param configFile XML configuration file
     * @param <T> type of returned object
     * @return new object
     * @since 2.0.0
     * @deprecated Use {@link #toObject(Path, Class)} instead
     */
    @Deprecated(since = "3.0.0")
    public <T> T loadFromXML(Path configFile) {
        return loadFromXML(configFile, null, null);
    }

    /**
     * Loads an XML configuration file and populates a new object
     * represented by the given "class" attribute found on XML root element.
     * @param configFile XML configuration file
     * @param errorHandler XML error handler
     * @param <T> type of returned object
     * @return new object
     * @since 2.0.0
     * @deprecated Use {@link #toObject(Path, Class)} instead
     */
    @Deprecated(since = "3.0.0")
    public <T> T loadFromXML(Path configFile, ErrorHandler errorHandler) {
        return loadFromXML(configFile, null, errorHandler);
    }

    /**
     * Loads an XML configuration file and populates a new object
     * represented by the given class.
     * @param configFile XML configuration file
     * @param objClass type of object to create and populate
     * @param <T> type of returned object
     * @return new object
     * @since 2.0.0
     * @deprecated Use {@link #toObject(Path, Class)} instead
     */
    @Deprecated(since = "3.0.0")
    public <T> T loadFromXML(Path configFile, Class<T> objClass) {
        return loadFromXML(configFile, objClass, null);
    }

    /**
     * Loads an XML configuration file and populates a new object
     * represented by the given class.
     * @param configFile XML configuration file
     * @param objClass type of object to create and populate
     * @param errorHandler XML error handler
     * @param <T> type of returned object
     * @return new object
     * @since 2.0.0
     * @deprecated Use {@link #toObject(Path, Class)} instead
     */
    @Deprecated(since = "3.0.0")
    public <T> T loadFromXML(
            Path configFile, Class<T> objClass, ErrorHandler errorHandler) {
        var xml = loadXML(configFile, errorHandler);
        if (xml == null) {
            return null;
        }
        if (objClass == null) {
            return xml.toObject();
        }
        T obj;
        try {
            obj = objClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(
                    "This class could not be instantiated: " + objClass, e);
        }
        loadFromXML(configFile, obj, errorHandler);
        return obj;
    }

    /**
     * Loads an XML configuration file and populates a given object.
     * @param configFile XML configuration file
     * @param object object to populate
     * @since 2.0.0
     * @deprecated Use {@link #toObject(Path, Object)} instead
     */
    @Deprecated(since = "3.0.0")
    public void loadFromXML(Path configFile, Object object) {
        loadFromXML(configFile, object, null);
    }

    /**
     * Loads an XML configuration file and populates a given object.
     * @param configFile XML configuration file
     * @param object object to populate
     * @param errorHandler XML error handler
     * @since 2.0.0
     * @deprecated Use {@link #toObject(Path, Object)} instead
     */
    @Deprecated(since = "3.0.0")
    public void loadFromXML(
            Path configFile, Object object, ErrorHandler errorHandler) {
        Objects.requireNonNull("'object' must not be null.");
        var xml = loadXML(configFile, errorHandler);
        if (xml != null) {
            xml.populate(object);
        }
    }

    /**
     * Loads a configuration file as a string.
     * @param configFile configuration file
     * @return configuration as string
     * @since 2.0.0
     * @deprecated Use {@link #toString(Path)} instead
     */
    @Deprecated(since = "3.0.0")
    public String loadString(Path configFile) {
        return toString(configFile);
    }

    //--- Private methods ----------------------------------------------------

    private static VelocityEngine createDefaultVelocityEngine() {
        var engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.EVENTHANDLER_INCLUDE,
                RelativeIncludeEventHandler.class.getName());
        engine.setProperty(RuntimeConstants.EVENTHANDLER_REFERENCEINSERTION,
                ExtendedReferenceInsertionEventHandler.class.getName());
        engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file");
        engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "");
        engine.setProperty(RuntimeConstants.INPUT_ENCODING,
                StandardCharsets.UTF_8.toString());
        engine.setProperty(RuntimeConstants.ENCODING_DEFAULT,
                StandardCharsets.UTF_8.toString());

        engine.setProperty("runtime.log", "");
        return engine;
    }
}
<fim_middle>