<filename>ondex-desktop/ovtk2/src/main/java/net/sourceforge/ondex/ovtk2/ui/popup/custom/CustomPopupItemBean.java<fim_prefix>

        package net.sourceforge.ondex.ovtk2.ui.popup.custom;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang.ArrayUtils;
import org.clapper.util.io.DirectoryFilter;

import net.sourceforge.ondex.ovtk2.config.Config;

/**
 * This is a JavaBean representing a single custom popup item.
 *
 * @author Martin Rittweger
 */
public class CustomPopupItemBean {

    public CustomPopupItemBean() {
        // for JavaBean
    }

    /**
     * @return A list of popup item names. If those are nested, the names
     *         contain the {@link File#separatorChar}. The names do not end with
     *         the ".xml" extension.
     */
    public static String[] getAvailablePopupItemNames() {
        File[] list = getAvailablePopupItemFiles();
        String[] itemNames = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            itemNames[i] = getQualifiedName(list[i]);
        }
        return itemNames;
    }

    /**
     * @return A list of xml files that could be found in the config/popupMenu/
     *         subdir. It also includes all subdirectories.
     */
    public static File[] getAvailablePopupItemFiles() {
        return getAvailablePopupItemFiles(getPopupPath());
    }

    private static File[] getAvailablePopupItemFiles(File path) {

        // get items of current path
        File[] list = path.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xml");
            }
        });

        // recursivly append files of subdirs to list
        for (File subdir : path.listFiles(new DirectoryFilter())) {
            File[] files = getAvailablePopupItemFiles(subdir);
            list = (File[]) ArrayUtils.addAll(list, files);
        }

        return list;
    }

    /**
     * @return Path to <code>Config.ovtkDir / popupMenu /</code>.
     */
    public static File getPopupPath() {
        File popupDir = new File(Config.ovtkDir + File.separatorChar + "popupMenu" + File.separatorChar);
        if (!popupDir.exists())
            return popupDir.mkdir() ? popupDir : null;
        if (popupDir.isDirectory())
            return popupDir;
        return null;
    }

    /**
     * @param fileName
     *            relative to the popupMenu path
     * @return
     * @throws FileNotFoundException
     * @throws java.util.NoSuchElementException
     */
    public static CustomPopupItemBean loadXML(String fileName) throws FileNotFoundException, java.util.NoSuchElementException {
        return loadXML(new File(getPopupPath().getAbsolutePath() + File.separatorChar + fileName));
    }

    public static CustomPopupItemBean loadXML(File xmlFile) throws FileNotFoundException, java.util.NoSuchElementException {
        XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(xmlFile)));
        CustomPopupItemBean item = (CustomPopupItemBean) d.readObject();
        d.close();
        // changing the file name and/or directory renames this object
        item.name = getQualifiedName(xmlFile);
        System.out.println(item.name);
        return item;
    }

    public static CustomPopupItemBean loadXML(URL xmlFile) throws java.util.NoSuchElementException, IOException {
<fim_suffix>
        // changing the file name and/or directory renames this object
        item.name = getQualifiedName(xmlFile);
        return item;
    }

    /**
     * Full name of this item: with File.separatorChar if this item is in a
     * submenu. This is without ".xml" and relative to the popupMenu/ path.
     */

    public String getQualifiedName() {
        return getName();
    }

    private static String getQualifiedName(File xmlFile) {
        String name = xmlFile.getPath().substring(xmlFile.getPath().indexOf(getPopupPath().toString()) + getPopupPath().toString().length() + 1);
        return name.substring(0, name.length() - 4);
    }

    /**
     * For use in applet, process URLs
     *
     * @param xmlFile
     * @return
     */
    private static String getQualifiedName(URL xmlFile) {
        String popupPath = Config.ovtkDir + "/popupMenu";
        String name = xmlFile.toString().substring(xmlFile.toString().indexOf(popupPath) + popupPath.length() + 1);
        name = name.substring(0, name.length() - 4);
        return name;
    }

    private static String getFilename(String name) {
        return getPopupPath().toString() + File.separatorChar + name + ".xml";
    }

    /**
     * Save this java bean as xml.
     *
     * @throws FileNotFoundException
     */
    public void saveXML() throws FileNotFoundException {

        // create subfolders of this item
        String regex = String.valueOf(File.separatorChar);
        if (regex.equals("\\"))
            regex = "\\\\";
        String[] split = getQualifiedName().split(regex);
        String dir = getPopupPath().toString();
        for (int i = 0; i < split.length - 1; i++) {
            dir += File.separatorChar + split[i];
            new File(dir).mkdir();
        }

        // save as xml
        XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(getFilename(name))));
        e.writeObject(this);
        e.close();
    }

    private String name = "";
	private String code = "";
    private String conceptClasses = "";
	private String libraries = "";

    /**
     * Delete the xml file backing this object.
     */
    public void delete() {
        new File(getFilename(name)).delete();
    }

    /*
     * getters and setters (required for JavaBean)
     */
    public String getName() {
        if (name.contains("%"))
            name = name.replaceAll("%20", " ");
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /** comma separated */
    public String getConceptClasses() {
        return conceptClasses;
    }

    public void setConceptClasses(String conceptClasses) {
        this.conceptClasses = conceptClasses;
    }

    public void setLibraries(String libraries) {
        this.libraries = libraries;
    }

    public String getLibraries() {
        return libraries;
    }

}
<fim_middle>