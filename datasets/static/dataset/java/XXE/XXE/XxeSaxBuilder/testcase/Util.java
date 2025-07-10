<filename>main/src/net/sourceforge/cruisecontrol/util/Util.java<fim_prefix>

package net.sourceforge.cruisecontrol.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.cruisecontrol.CruiseControlException;

import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

public final class Util {

    private Util() {
    }

    public static Element loadRootElement(File configFile) throws CruiseControlException {
        try {
            SAXBuilder builder = new SAXBuilder();
            <fim_suffix>
        } catch (Exception e) {
            throw new CruiseControlException(
                    "failed to load file [" + (configFile != null
                            ? configFile.getName()
                            : "") + "]",
                    e);
        }
    }

    public static Element loadRootElement(InputStream in) throws CruiseControlException {
        try {
            SAXBuilder builder = new SAXBuilder();
            return builder.build(in).getRootElement();
        } catch (Exception e) {
            throw new CruiseControlException("failed to parse configuration", e);
        }
    }

    public static boolean isWindows() {
        String osName = Util.getOsName();
        return osName.indexOf("Windows") > -1;
    }

    public static String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * Loads a set of properties from the specified properties file. The file
     * must exist and be in the proper format. If not, a
     * <code>CruiseControlException</code> is thrown.
     *
     * @param file
     *            The <code>File</code> from which to load the properties
     * @return A <code>Properties</code> object which contains all properties
     *         defined in the file.
     * @throws CruiseControlException wraps FileNotFoundException
     * @throws IOException if properties fail to load
     */
    public static Properties loadPropertiesFromFile(File file)
            throws CruiseControlException, IOException {
        Properties properties = new Properties();

        // Load the properties from file

        try {
            final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            try {
                properties.load(bis);
            } finally {
                IO.close(bis);
            }
        } catch (FileNotFoundException e) {
            throw new CruiseControlException(
                    "Could not load properties from file "
                            + file.getAbsolutePath() + ". It does not exist.",
                    e);
        }

        return properties;
    }

    /**
     * Stores the contents of a <code>Properties</code> object to the specifed
     * file. If the file does not exist, it will be created (if possible).
     *
     * @param properties
     *            The <code>Properties</code> object which will be stored to
     *            file
     * @param header
     *            A string which will be written to the first line of the
     *            properties file as a comment. Can be <code>null</code>.
     * @param file
     *            The properties file to which the properties will be written.
     *
     * @throws CruiseControlException wraps FileNotFoundException
     * @throws IOException if properties fail to store
     */
    public static void storePropertiesToFile(final Properties properties,
                                             final String header, final File file)
            throws CruiseControlException, IOException {

        try {
            final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            try {
                properties.store(bos, header);
            } finally {
                IO.close(bos);
            }
        } catch (FileNotFoundException e) {
            throw new CruiseControlException(
                    "Could not store properties to file "
                            + file.getAbsolutePath() + ". It does not exist.",
                    e);
        }
    }

    /**
     * Return the content of the file specified by its path into a <code>String</code>
     * @param fileName the file to read
     * @return the content of the file specified by its path into a <code>String</code>
     * @throws IOException if io error occurs
     */
    public static String readFileToString(final String fileName) throws IOException {
        final StringBuilder out = new StringBuilder();
        appendFileToBuffer(fileName, out);
        return out.toString();
    }

    public static String readFileToString(final File file) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final StringBuilder result = new StringBuilder();

        try {
            String s = reader.readLine();
            while (s != null) {
                result.append(s.trim());
                s = reader.readLine();
            }
        } finally {
            reader.close();
        }

        return result.toString();
    }

    /**
     * Append the content of the file specified by its path into a <code>StringBuilder</code>
     * @param fileName file who's content is to be appeneded
     * @param out buffer onto which to append the file content
     * @throws IOException if io error occurs
     */
    public static void appendFileToBuffer(final String fileName, final StringBuilder out) throws IOException {
        final FileReader fr = new FileReader(fileName);
        try {
            char[] buff = new char[4096];
            int size = fr.read(buff, 0, 4096);
            while (size > 0) {
                out.append(buff, 0, size);
                size = fr.read(buff, 0, 4096);
            }
        } finally {
            IO.close(fr);
        }
    }

    /**
     * Attempt to fix possible race condition when creating directories on
     * WinXP, also Windows2000. If the mkdirs does not work, wait a little and
     * try again.
     * Taken from Ant Mkdir taskdef.
     *
     * @param f the path for which directories are to be created
     * @return <code>true</code> if and only if the directory was created,
     *         along with all necessary parent directories; <code>false</code>
     *         otherwise
     */
    public static boolean doMkDirs(File f) {
        if (!f.mkdirs()) {
            try {
                Thread.sleep(10);
                return f.mkdirs();
            } catch (InterruptedException ex) {
                return f.mkdirs();
            }
        }
        return true;
    }

    /**
     * Parses a string by replacing all occurrences of a property macro with
     * the resolved value of the property. Nested macros are allowed - the
     * inner most macro will be resolved first, moving out from there.
     *
     * @param props the properties to search for
     * @param string The string to be parsed
     * @param failIfMissing if true, fail if the property is not defined
     * @return The parsed string
     * @throws CruiseControlException if a property cannot be resolved
     */
    public static String parsePropertiesInString(final Map<String, String> props,
                                                 String string,
                                                 final boolean failIfMissing)
            throws CruiseControlException {

        if (string != null) {
            final int startIndex = string.indexOf("${");
            if (startIndex != -1) {
                int openedBrackets = 1;
                int lastStartIndex = startIndex + 2;
                int endIndex;
                do {
                    endIndex = string.indexOf("}", lastStartIndex);
                    int otherStartIndex = string.indexOf("${", lastStartIndex);
                    if (otherStartIndex != -1 && otherStartIndex < endIndex) {
                        openedBrackets++;
                        lastStartIndex = otherStartIndex + 2;
                    } else {
                        openedBrackets--;
                        if (openedBrackets == 0) {
                            break;
                        }
                        lastStartIndex = endIndex + 1;
                    }
                } while (true);
                if (endIndex < startIndex + 2) {
                    throw new CruiseControlException("Unclosed brackets in " + string);
                }
                final String property = string.substring(startIndex + 2, endIndex);
                // not necessarily resolved
                final String propertyName = parsePropertiesInString(props, property, failIfMissing);
                String value = "".equals(propertyName) ? "" : props.get(propertyName);
                if (value == null) {
                    if (failIfMissing) {
                        throw new CruiseControlException("Property \"" + propertyName
                                + "\" is not defined. Please check the order in which you have used your properties.");
                    } else {
                        // we don't resolve missing properties
                        value = "${" + propertyName + "}";
                    }
                } else { // Try one more to resolve recursive properties
                    String key = "!!!" + value + "!!!";
                    // If the property has already been resolved, ignore it here. It prevents
                    // infinite properties recursion
                    if (!props.containsKey(key)) {
                        final Map<String, String> props2 = new HashMap<String, String>(props);
                        props2.put(key, "");
                        value = parsePropertiesInString(props2, value, failIfMissing);
                    }
                }
                string = string.substring(0, startIndex) + value
                        + parsePropertiesInString(props, string.substring(endIndex + 1), failIfMissing);
            }
        }
        return string;
    }
}
<fim_middle>