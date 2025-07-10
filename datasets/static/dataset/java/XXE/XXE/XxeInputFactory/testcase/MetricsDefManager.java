<filename>myperf/src/main/java/com/yahoo/dba/perf/myperf/common/MetricsDefManager.java<fim_prefix>

/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.common;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/**
 * A collection of metrics definitions.
 * @author xrao
 *
 */
public class MetricsDefManager implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;


    private static Logger logger = Logger.getLogger(MetricsDefManager.class.getName());
    private static String GROUP_TAG = "metricsGroup";
    private static String SUBGROUP_TAG = "subgroup";
    private static String METRIC_TAG = "metric";
    private static String BUILTIN_METRICS_FILE_NAME = "metrics.xml";

    // Metrics definition. the key is the group name
    private java.util.Map<String, MetricsGroup> metricsGroups = new java.util.LinkedHashMap<String, MetricsGroup>();

    //user defined metrics
    private UDMManager udmManager = new UDMManager();

    public MetricsDefManager()
    {

    }

    public UDMManager getUdmManager() {
        return udmManager;
    }

    synchronized public String[] getGroupNames()
    {

        return metricsGroups.keySet().toArray(new String[0]);
    }

    synchronized public MetricsGroup getGroupByName(String name)
    {
        if (this.metricsGroups.containsKey(name))
            return this.metricsGroups.get(name);
        else
            return null;
    }

    synchronized void addGroup(MetricsGroup group)
    {
        if (group != null && group.getGroupName() != null
                && !group.getGroupName().isEmpty())
            this.metricsGroups.put(group.getGroupName(), group);
    }

    /**
     * Return a copy of requested metrics info
     * @param grpName
     * @param subGrpName
     * @param ms
     * @return
     */
    synchronized public List<Metric> getMetrics(String grpName, String subGrpName, String[] ms)
    {
        MetricsGroup mg = null;
        if(grpName != null && grpName.startsWith("UDM_"))
        {
            try{
                mg = this.udmManager.getUDMByName(grpName.substring(grpName.indexOf('_')+1)).getMetricsGroup();
            }catch(Exception ex){}
        }
        else
            mg = this.metricsGroups.get(grpName);
        if (mg == null)return null;
        if (subGrpName != null && !subGrpName.isEmpty() && !subGrpName.equals("_") && !grpName.startsWith("UDM_"))
        {
            mg = mg.getSubGroupByName(subGrpName);
        }
        if (mg == null)return null;

        HashMap<String, Metric> mmap = new HashMap<String, Metric>(mg.getMetrics().size());
        for(Metric m: mg.getMetrics())
            mmap.put(m.getName(), m);
        List<Metric> mList = new java.util.ArrayList<Metric>(ms.length);
        for(String mname: ms)
        {
            if(mmap.containsKey(mname))
                mList.add(mmap.get(mname).copy());
        }
        return mList;
    }
    public void init()
    {
        //empty the metrics definitions and reload
        this.metricsGroups.clear();
        load();
        this.udmManager.init();
    }

    private void load()
    {
        logger.info("Loading metrics definitions ...");
        //for now, we only load built in metrics
        java.io.InputStream in = null;
        try
        {
            in = this.getClass().getClassLoader().getResourceAsStream(BUILTIN_METRICS_FILE_NAME);
            if (in != null)
            {
                List<MetricsGroup> groups = load(in);
                if (groups != null)
                {
                    for (MetricsGroup mg: groups)
                        addGroup(mg);
                }
            }
        }
        catch(Exception ex)
        {
            logger.log(Level.WARNING, "Error parsing builtin "+BUILTIN_METRICS_FILE_NAME, ex);
        }
        finally
        {
            if(in!=null)try{in.close(); in=null;}catch(Exception iex){}
        }
        logger.info("Done loading metrics definitiond: " + this.metricsGroups.size());
    }

    //make it generic so that later we can extend to load external definitions
    private List<MetricsGroup> load(InputStream in)
    {
        List<MetricsGroup> groups = new java.util.ArrayList<MetricsGroup>();
        if (in == null)return groups;

        XMLStreamReader reader = null;
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();//will be used only once at startup time
        try
        {
            <fim_suffix>
            reader = inputFactory.createXMLStreamReader(new java.io.InputStreamReader(in));
            while(reader.hasNext())
            {
                int evtType = reader.next();
                //try{logger.info(evtType+"");logger.info(reader.getLocalName());}catch(Exception ex){}
                if(evtType!=XMLStreamConstants.START_ELEMENT)continue;
                String tagName = reader.getLocalName();
                if(!GROUP_TAG.equals(tagName))continue;
                MetricsGroup mg = null;
                if(mg != null)
                {
                    try
                    {
                        while(reader.hasNext())
                        {
                            int evtType2 = reader.next();
                            if(evtType2==XMLStreamConstants.END_ELEMENT && GROUP_TAG.equals(reader.getLocalName()))break;
                            if(evtType2!=XMLStreamConstants.START_ELEMENT)continue;
                            String tagName2 = reader.getLocalName();
                            if(METRIC_TAG.equalsIgnoreCase(tagName2))
                            {
                                Metric m = null;
                                if (m != null) mg.addMetrics(m);
                            }else if ("sqlText".equalsIgnoreCase(tagName2)) //only top level MetricsGroup allows sqlText
                                mg.setSqlText(reader.getElementText());
                            else if (SUBGROUP_TAG.equalsIgnoreCase(tagName2))
                            {
                                //now parse subgroup
                                MetricsGroup subGroup = this.parseSubGroup(reader);
                                if (subGroup != null) mg.addSubGroups(subGroup);
                            }
                        }
                    }catch(Exception ex)
                    {
                        logger.log(Level.WARNING, "Error parsing metrics.xml", ex);
                    }
                    logger.info("Add metric group "+mg.getGroupName()+" for "+mg.getDbType());
                    groups.add(mg);
                }else
                {
                    logger.warning("Read metricsGroup without name attribute");
                }
            }
        }
        catch(Exception ex)
        {
            logger.log(Level.WARNING, "Error parsing metrics.xml", ex);
        }
        finally
        {
            if(reader!=null)try{reader.close(); reader=null;}catch(Exception iex){}
        }

        return groups;
    }

    private MetricsGroup parseSubGroup(XMLStreamReader reader)
    {
        //assume tag subGroup
        MetricsGroup mg = null;
        if(mg!= null )
        {
            try
            {
                while(reader.hasNext())
                {
                    int evtType2 = reader.next();
                    if(evtType2 == XMLStreamConstants.END_ELEMENT
                            && SUBGROUP_TAG.equals(reader.getLocalName()))
                        break;

                    if(evtType2 != XMLStreamConstants.START_ELEMENT) continue;

                    String tagName2 = reader.getLocalName();
                    if(METRIC_TAG.equalsIgnoreCase(tagName2))
                    {
                        Metric m = null;
                        if (m != null) mg.addMetrics(m);
                    }
                    //others are ignored
                }
            }catch(Exception ex)
            {
                logger.log(Level.WARNING, "Error parsing metrics.xml for sub Group "+mg.getGroupName(), ex);
            }
        }
        return mg;
    }
}
<fim_middle>
