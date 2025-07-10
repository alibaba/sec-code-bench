<filename>DataSourceHandler.java<fim_prefix>

/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

 package tufts.vue;

 import tufts.Util;
 import tufts.vue.gui.GUI;
 import tufts.vue.gui.VueButton;
 import tufts.vue.gui.Widget;
 import tufts.vue.ui.MetaDataPane;
 import tufts.vue.ui.ResourceList;
 
 
 import javax.swing.*;
 import javax.swing.event.*;
 import javax.swing.table.DefaultTableModel;
 import javax.swing.border.*;
 import java.awt.*;
 import java.awt.event.*;
 import java.util.Locale;
 import java.util.Vector;
 import java.io.*;
 import java.util.*;
 import java.net.URL;
 
 import edu.tufts.vue.dsm.impl.VueDataSource;
 
 import org.osid.repository.Repository;
 import org.osid.repository.RepositoryException;
 
 // castor classes
 import org.exolab.castor.xml.Marshaller;
 import org.exolab.castor.xml.Unmarshaller;
 import org.exolab.castor.xml.MarshalException;
 import org.exolab.castor.xml.ValidationException;
 
 import org.exolab.castor.mapping.Mapping;
 import org.exolab.castor.mapping.MappingException;
 import org.osid.provider.ProviderException;
 import org.xml.sax.InputSource;
 
 import tufts.vue.gui.*;
 
 import edu.tufts.vue.dsm.impl.VueDataSourceManager;
 
 /**
  * This class wraps a DataSourceList, and handles communicating user
  * selection events on the list to other panes, such as search or browse,
  * as well as requesting browse components and loading them into the browse pane.
  * Also handles the big task of kicking off parallel searches for multiple sources
  * in multiple threads (full parallel federated search).
  */
 
 // TODO: in order to keep backward compat with old DataSourceViewer code (unless we're
 // ready to just throw the swtich), we'll need an interface that both DataSourceViewer
 // and DataSourceHandler can implement, that includes at least a setActiveDataSource for
 // the two types, plus addOrdered / getModelContents, or some new pairs of clearear and
 // more symmetrical calls that allows adding/remove items from the list.
 
 public class DataSourceHandler extends JPanel
     implements edu.tufts.vue.dsm.DataSourceListener,
                edu.tufts.vue.fsm.event.SearchListener,
                KeyListener,
                ActionListener
 {
     private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(DataSourceHandler.class);
     //private static final boolean UseFederatedSearchManager = false;
     
     private final DataFinder DRB;
     private Object activeDataSource;
     private tufts.vue.BrowseDataSource browserDS;
     
     public final static int ADD_MODE = 0;
     public final static int EDIT_MODE = 1;
     public final static org.osid.shared.Type favoritesRepositoryType = new edu.tufts.vue.util.Type("edu.tufts","favorites","Favorites");
     
     private JPopupMenu popup;
     
     private static AddLibraryDialog addLibraryDialog;
     private static UpdateLibraryDialog updateLibraryDialog;
     
     private static AbstractAction checkForUpdatesAction; // can these really be static?
     private static AbstractAction addLibraryAction;
     private AbstractAction editLibraryAction; // should these be static?
     private AbstractAction removeLibraryAction;
     
     //public static DataSourceList dataSourceList;
     public BasicSourcesList dataSourceList;
     
     private static DockWindow editInfoDockWindow; // hack for now: need this set before DSV is created
 
     private edu.tufts.vue.dsm.DataSourceManager dataSourceManager;
     private edu.tufts.vue.fsm.FederatedSearchManager federatedSearchManager;
     private edu.tufts.vue.fsm.QueryEditor queryEditor;
     private edu.tufts.vue.fsm.SourcesAndTypesManager sourcesAndTypesManager;
     
     private final org.osid.shared.Type searchType = new edu.tufts.vue.util.Type("mit.edu","search","keyword");
     private final org.osid.shared.Type thumbnailType = new edu.tufts.vue.util.Type("mit.edu","partStructure","thumbnail");
     
     private org.osid.OsidContext context = new org.osid.OsidContext();
     //org.osid.registry.Provider checked[];
     
     private final java.util.List<SearchThread> mSearchThreads = java.util.Collections.synchronizedList(new java.util.LinkedList<SearchThread>());
 
     //private static DataSourceHandler singleton;
 
     private class BasicSourcesList extends JList {
 
         //private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(BasicSourcesList.class);
     
         public BasicSourcesList() {
             super(new DefaultListModel());
             this.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
             this.setFixedCellHeight(-1);
             this.setCellRenderer(new DataSourceListCellRenderer());
         }
 
         public DefaultListModel getModelContents() {
             return (DefaultListModel) getModel();
         }
         // todo: does not maintain groupings on later OSID add update should list contain both OSIDs & VUE local DataSources
         public boolean addOrdered(Object o) {
             if (excludedSources.contains(o.getClass()))
                 return false;
             if (includedSources.size() > 0 && !includedSources.contains(o.getClass()))
                 return false;
             final DefaultListModel model = getModelContents();
             if (!model.contains(o)) {
                 model.addElement(o);
                 return true;
             } else {
                 return false;
             }
         }
     }
 
 
     private final Set<Class> includedSources = new HashSet();
     private final Set<Class> excludedSources = new HashSet();
     

    
 
     
     public  SaveDataSourceViewer unMarshallMap(File file)
     throws java.io.IOException,
             org.exolab.castor.xml.MarshalException,
             org.exolab.castor.mapping.MappingException,
             org.exolab.castor.xml.ValidationException {
         Unmarshaller unmarshaller = new Unmarshaller();
         FileReader reader = new FileReader(file);
         <fim_suffix>
         reader.close();
         return sviewer;
     }
    

 }
<fim_middle>