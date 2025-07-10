<filename>modules/org.pathvisio.core/src/org/pathvisio/core/model/GpmlFormat.java<fim_prefix>

/*******************************************************************************
 * PathVisio, a tool for data visualization and analysis using biological pathways
 * Copyright 2006-2019 BiGCaT Bioinformatics
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.pathvisio.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import org.bridgedb.DataSource;
import org.bridgedb.bio.DataSourceTxt;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.JDOMParseException;
import org.jdom2.input.SAXBuilder;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.util.RootElementFinder;
import org.xml.sax.InputSource;


/**
 * class responsible for interaction with Gpml format.
 * Contains all gpml-specific constants,
 * and should be the only class (apart from svgFormat)
 * that needs to import jdom
 */
public class GpmlFormat extends AbstractPathwayFormat
{
    static private final GpmlFormat2013a CURRENT = GpmlFormat2013a.GPML_2013A;

    public static final Namespace RDF = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    public static final Namespace RDFS = Namespace.getNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    public static final Namespace BIOPAX = Namespace.getNamespace("bp", "http://www.biopax.org/release/biopax-level3.owl#");
    public static final Namespace OWL = Namespace.getNamespace("owl", "http://www.w3.org/2002/07/owl#");

    static {
        if (!DataSource.fullNameExists("Ensembl")) DataSourceTxt.init();
    }

    public Pathway doImport(File file) throws ConverterException
    {
        Pathway pathway = new Pathway();
        readFromXml(pathway, file, true);
        pathway.clearChangedFlag();
        return pathway;
    }

    public void doExport(File file, Pathway pathway) throws ConverterException
    {
        writeToXml(pathway, file, true);
    }

    public String[] getExtensions() {
        return new String[] { "gpml", "xml" };
    }

    public String getName() {
        return "GPML file";
    }

    public static Document createJdom(Pathway data) throws ConverterException
    {
        return CURRENT.createJdom(data);
    }

    static public Element createJdomElement(PathwayElement o) throws ConverterException
    {
        return CURRENT.createJdomElement(o);
    }

    public static PathwayElement mapElement(Element e) throws ConverterException
    {
        return CURRENT.mapElement(e);
    }

    /**
     * Writes the JDOM document to the file specified
     * @param file	the file to which the JDOM document should be saved
     * @param validate if true, validate the dom structure before writing to file. If there is a validation error,
     * 		or the xsd is not in the classpath, an exception will be thrown.
     */
    static public void writeToXml(Pathway pwy, File file, boolean validate) throws ConverterException
    {
        CURRENT.writeToXml(pwy, file, validate);
    }

    static public void writeToXml(Pathway pwy, OutputStream out, boolean validate) throws ConverterException
    {
        CURRENT.writeToXml(pwy, out, validate);
    }

    static public void readFromXml(Pathway pwy, File file, boolean validate) throws ConverterException
    {
        InputStream inf;
        try
        {
            inf = new FileInputStream (file);
        }
        catch (FileNotFoundException e)
        {
            throw new ConverterException (e);
        }
        readFromXmlImpl (pwy, new InputSource(inf), validate);
    }

    static public void readFromXml(Pathway pwy, InputStream in, boolean validate) throws ConverterException
    {
        readFromXmlImpl (pwy, new InputSource(in), validate);
    }

    static public void readFromXml(Pathway pwy, Reader in, boolean validate) throws ConverterException
    {
        readFromXmlImpl (pwy, new InputSource(in), validate);
    }

    public static GpmlFormatReader getReaderForNamespace (Namespace ns)
    {
        GpmlFormatReader[] formats = new GpmlFormatReader[]
                {
                        GpmlFormat200X.GPML_2007, GpmlFormat200X.GPML_2008A, GpmlFormat2010a.GPML_2010A , GpmlFormat2013a.GPML_2013A
                };
        for (GpmlFormatReader format : formats)
        {
            if (ns.equals(format.getGpmlNamespace()))
            {
                return format;
            }
        }
        return null;
    }

    private static void readFromXmlImpl(Pathway pwy, InputSource in, boolean validate) throws ConverterException
    {
        // Start XML processing

        SAXBuilder builder  = new SAXBuilder(false); // no validation when reading the xml file
        <fim_suffix>
        // try to read the file; if an error occurs, catch the exception and print feedback
        try
        {
            Logger.log.trace ("Build JDOM tree");
            // build JDOM tree
            Document doc = builder.build(in);

            // Copy the pathway information to a VPathway
            Element root = doc.getRootElement();
            if (!root.getName().equals("Pathway"))
            {
                throw new ConverterException ("Not a Pathway file");
            }

            Namespace ns = root.getNamespace();
            GpmlFormatReader format = getReaderForNamespace (ns);
            if (format == null)
            {
                throw new ConverterException ("This file looks like a pathway, " +
                        "but the namespace " + ns + " was not recognized. This application might be out of date.");
            }
            Logger.log.info ("Recognized format " + ns);

            Logger.log.trace ("Start Validation");
            if (validate) format.validateDocument(doc);
            Logger.log.trace ("Copy map elements");

            format.readFromRoot (root, pwy);
        }
        catch(JDOMParseException pe)
        {
            throw new ConverterException (pe);
        }
        catch(JDOMException e)
        {
            throw new ConverterException (e);
        }
        catch(IOException e)
        {
            throw new ConverterException (e);
        }
        catch(NullPointerException e)
        {
            throw new ConverterException (e);
        }
        catch(IllegalArgumentException e) {
            throw new ConverterException (e);
        }
        catch(Exception e) { //Make all types of exceptions a ConverterException
            throw new ConverterException (e);
        }
    }

    @Override
    public boolean isCorrectType(File f)
    {
        String uri;
        try
        {
            uri = "" + RootElementFinder.getRootUri(f);
            return uri.startsWith ("http://genmapp.org/");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void doExport(File file, Pathway pathway, int zoom)
            throws ConverterException {
        // TODO Auto-generated method stub

    }

}
<fim_middle>