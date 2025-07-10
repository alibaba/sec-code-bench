<filename>ResourceID.java<fim_prefix>

/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.3.1</a>, using an XML
 * Schema.
 * $Id$
 */

 package eu.dca.model;

 /**
  * Definition: The unique numeric or alphanumeric identification of
  * the original (digital or analogue) resource.
  * 
  * @version $Revision$ $Date$
  */
 @SuppressWarnings("serial")
 public class ResourceID extends IdentifierComplexType 
 implements java.io.Serializable
 {
 
 
       //----------------/
      //- Constructors -/
     //----------------/
 
     public ResourceID() {
         super();
     }
 
     public ResourceID(final java.lang.String defaultValue) {
         super(defaultValue);
     }
 
 
       //-----------/
      //- Methods -/
     //-----------/
 
     /**
      * Method isValid.
      * 
      * @return true if this object is valid according to the schema
      */
     public boolean isValid(
     ) {
         try {
             validate();
         } catch (org.exolab.castor.xml.ValidationException vex) {
             return false;
         }
         return true;
     }
 
     /**
      * 
      * 
      * @param out
      * @throws org.exolab.castor.xml.MarshalException if object is
      * null or if any SAXException is thrown during marshaling
      * @throws org.exolab.castor.xml.ValidationException if this
      * object is an invalid instance according to the schema
      */
     public void marshal(
             final java.io.Writer out)
     throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
         org.exolab.castor.xml.Marshaller.marshal(this, out);
     }
 
     /**
      * 
      * 
      * @param handler
      * @throws java.io.IOException if an IOException occurs during
      * marshaling
      * @throws org.exolab.castor.xml.ValidationException if this
      * object is an invalid instance according to the schema
      * @throws org.exolab.castor.xml.MarshalException if object is
      * null or if any SAXException is thrown during marshaling
      */
     public void marshal(
             final org.xml.sax.ContentHandler handler)
     throws java.io.IOException, org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {
         org.exolab.castor.xml.Marshaller.marshal(this, handler);
     }
 
     /**
      * Method unmarshal.
      * 
      * @param reader
      * @throws org.exolab.castor.xml.MarshalException if object is
      * null or if any SAXException is thrown during marshaling
      * @throws org.exolab.castor.xml.ValidationException if this
      * object is an invalid instance according to the schema
      * @return the unmarshaled eu.dca.model.ResourceID
      */
     public static eu.dca.model.ResourceID unmarshal(
             final java.io.Reader reader)
     throws org.exolab.castor.xml.MarshalException, org.exolab.castor.xml.ValidationException {

        <fim_suffix>
    }
 
     /**
      * 
      * 
      * @throws org.exolab.castor.xml.ValidationException if this
      * object is an invalid instance according to the schema
      */
     public void validate(
     )
     throws org.exolab.castor.xml.ValidationException {
         org.exolab.castor.xml.Validator validator = new org.exolab.castor.xml.Validator();
         validator.validate(this);
     }
 
 }
<fim_middle>