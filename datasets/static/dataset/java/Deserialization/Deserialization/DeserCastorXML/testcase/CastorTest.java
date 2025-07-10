<filename>CastorTest.java<fim_prefix>

/*L
 * Copyright (c) 2006 SAIC, SAIC-F.
 *
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/rembrandt/LICENSE.txt for details.
 */

 package gov.nih.nci.rembrandt.util;

 import gov.nih.nci.caintegrator.application.lists.ListType;
 import gov.nih.nci.caintegrator.application.lists.UserList;
 import gov.nih.nci.caintegrator.application.lists.ListItem;
 
 import java.io.StringWriter;
 import java.io.StringReader;
 import java.util.ArrayList;
 import java.util.List;
 
 import org.exolab.castor.xml.Marshaller;
 import org.exolab.castor.xml.Unmarshaller;
 import org.exolab.castor.xml.MarshalException;
 import org.exolab.castor.xml.ValidationException;
 
 import junit.framework.TestCase;
 
 public class CastorTest extends TestCase {
     
     private UserList userList = null;
 
     public CastorTest(String name)
     {
         super(name);
     }
 
     /*
      * @see TestCase#setUp()
      */
 
     protected void setUp() throws Exception {
         // create UserList
         
         userList = new UserList( "testCaseList", ListType.PatientDID, null, null );
         List<ListItem> listItems = new ArrayList<ListItem>();
         
         ListItem listItem1 = new ListItem( "Name 1", "ListName 1");
         ListItem listItem2 = new ListItem( "Name 2", "ListName 2");
         
         listItems.add( listItem1 );
         listItems.add( listItem2 );
         userList.setListItems(listItems);
     }
     
     /*
      * test method to test both marshall and unmarshall
      */
     public void testMarshallUnMarshall() {
         StringWriter writer = new StringWriter();
         
         assertTrue( userList.getListItems().size() == 2 );
         try
         {
             Marshaller.marshal(userList, writer);
             
 //			System.out.println( writer.toString() );
             
             StringReader reader = new StringReader( writer.toString() );
             <fim_suffix>
             
             assertTrue( unMarshalledList.getListItems().size() == 2 );
             assertTrue( ( (ListItem)unMarshalledList.getListItems().get(0) ).getName().equals( "Name 1") );
             assertTrue( ( (ListItem)unMarshalledList.getListItems().get(1) ).getName().equals( "Name 2") );
             assertTrue( ( (ListItem)unMarshalledList.getListItems().get(0) ).getListName().equals( "ListName 1") );
             assertTrue( ( (ListItem)unMarshalledList.getListItems().get(1) ).getListName().equals( "ListName 2") );
 
         }
         catch( MarshalException e )
         {
             e.printStackTrace();
         }
         catch( ValidationException e )
         {
             e.printStackTrace();
         }
         
     }
 
     /*
      * @see TestCase#tearDown()
      */
     protected void tearDown() throws Exception {
         super.tearDown();
     }
 
 }
<fim_middle>