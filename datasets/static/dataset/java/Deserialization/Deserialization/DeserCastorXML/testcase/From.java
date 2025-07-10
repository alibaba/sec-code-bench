<filename>From.java<fim_prefix>

/**
 * Copyright (c) 2004-2011 Wang Jinbao(Julian Wong), http://www.ralasafe.com
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 */
/*
 * This class was automatically generated with 
 * <a href="http://www.castor.org">Castor 1.2</a>, using an XML
 * Schema.
 * $Id: From.java,v 1.1 2010/07/09 08:17:07 back Exp $
 */

 package org.ralasafe.db.sql.xml;

 //---------------------------------/
//- Imported classes and packages -/
//---------------------------------/

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

/**
* Class From.
* 
* @version $Revision: 1.1 $ $Date: 2010/07/09 08:17:07 $
*/
public class From extends FromType 
implements java.io.Serializable
{


     //----------------/
    //- Constructors -/
   //----------------/

   public From() {
       super();
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
       Marshaller.marshal(this, out);
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
       Marshaller.marshal(this, handler);
   }

   /**
    * Method unmarshal.
    * 
    * @param reader
    * @throws org.exolab.castor.xml.MarshalException if object is
    * null or if any SAXException is thrown during marshaling
    * @throws org.exolab.castor.xml.ValidationException if this
    * object is an invalid instance according to the schema
    * @return the unmarshaled org.ralasafe.db.sql.xml.FromType
    */
   public static org.ralasafe.db.sql.xml.FromType unmarshal(
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