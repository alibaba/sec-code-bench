<filename>sopc2dts/lib/BoardInfo.java<fim_prefix>

/*
sopc2dts - Devicetree generation for Altera systems

Copyright (C) 2011 - 2015 Walter Goossens <waltergoossens@home.nl>

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package sopc2dts.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import sopc2dts.Logger;
import sopc2dts.Logger.LogLevel;
import sopc2dts.lib.Parameter;
import sopc2dts.lib.boardinfo.BICDTAppend;
import sopc2dts.lib.boardinfo.BICEthernet;
import sopc2dts.lib.boardinfo.BICI2C;
import sopc2dts.lib.boardinfo.BoardInfoComponent;
import sopc2dts.lib.boardinfo.I2CSlave;
import sopc2dts.lib.components.BasicComponent;
import sopc2dts.lib.components.base.FlashPartition;

public class BoardInfo implements ContentHandler {
    public enum PovType { CPU, PCI };
    public enum SortType { NONE, ADDRESS, NAME, LABEL };
    public enum RangesStyle { NONE, FOR_BRIDGE, FOR_EACH_CHILD };
    public enum AltrStyle { AUTO, FORCE_UPPER, FORCE_LOWER };
    FlashPartition part;
    private File sourceFile;
    String currTag;
    String flashChip;
    boolean includeTime = true;
    boolean showClockTree = false;
    boolean showConduits = false;
    boolean showResets = false;
    boolean showStreaming = false;
    Vector<FlashPartition> vPartitions;
    Vector<String> vMemoryNodes;
    Vector<BoardInfoComponent> vBics = new Vector<BoardInfoComponent>();
    Vector<Parameter> vAliases = new Vector<Parameter>();
    Vector<Parameter> vAliasRefs = new Vector<Parameter>();
    Vector<String> vIrqMasterClassIgnore = new Vector<String>();
    Vector<String> vIrqMasterLabelIgnore = new Vector<String>();

    String bootArgs;
    BoardInfoComponent currBic;
    private AltrStyle altrStyle = AltrStyle.AUTO;
    private String pov = "";
    private PovType povType = PovType.CPU;
    private RangesStyle rangesStyle = RangesStyle.FOR_EACH_CHILD;
    private SortType sortType = SortType.NONE;
    private BasicComponent.parameter_action dumpParameters = BasicComponent.parameter_action.NONE;
    HashMap<String, Vector<FlashPartition>> mFlashPartitions =
            new HashMap<String, Vector<FlashPartition>>(4);

    public BoardInfo()
    {
        vMemoryNodes = new Vector<String>();
    }
    public BoardInfo(File source) throws FileNotFoundException, SAXException, IOException
    {
        load(source);
    }
    public BoardInfo(InputSource in) throws SAXException, IOException
    {
        load(in);
    }
    public void load(File source) throws FileNotFoundException, SAXException, IOException
    {
        sourceFile = source;
        load(new InputSource(new BufferedReader(new FileReader(sourceFile))));
    }
    protected void load(InputSource in) throws SAXException, IOException
    {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        <fim_suffix>
        xmlReader.setContentHandler(this);
        xmlReader.parse(in);
    }

}

<fim_middle>