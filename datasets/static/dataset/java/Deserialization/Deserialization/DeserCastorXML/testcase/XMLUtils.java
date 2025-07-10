<filename>XMLUtils.java<fim_prefix>

package com.espaitic.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.XMLContext;
import org.xml.sax.InputSource;

import com.espaitic.bean.CallCenterDatosExtension;
import com.espaitic.bean.CallCenterDatosLlamada;
import com.espaitic.bean.CallCenterDatosNuevaLlamada;
import com.espaitic.bean.CallCenterDatosRedireccion;
import com.espaitic.bean.CallCenterLlamadas;
import com.espaitic.bean.CallCenterLogPeticioRegistre;
import com.espaitic.bean.CallCenterLogRespostaAlcUsuaris;
import com.espaitic.bean.CallCenterLogRespostaCallItem;
import com.espaitic.bean.CallCenterRespuesta;

import desertic.database.LlamadaAlcatel;

/**
 * Clase temporal para poder trabajar con XML
 * 
 * @author Jose M. Morles�n
 */
public class XMLUtils {
	
	private static Logger logger = Logger.getLogger(XMLUtils.class);
	
	private static final String MAPPING_FILE = "mapping.xml";
	
	private static XMLContext context = null;


	/**
	 * M�todo que convierte una instancia de una clase Serializable en un xml
	 * @param o
	 * @return
	 */
	public static String marshal(Object o){
		String xml = null;
		try {
			StringWriter sw = new StringWriter();
			Marshaller.marshal(o, sw);
			xml = sw.getBuffer().toString();
		} catch (MarshalException e) {
			logger.error(e.getMessage());
		} catch (ValidationException e) {
			logger.error(e.getMessage());
		}
		return xml;
	}
	
	/**
	 * M�todo que convierte un xml en una instancia de la clase a la que pertenece
	 * @param c
	 * @param xml
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Object unmarshall(Class c, String xml){
		Object o = null;
		try {
			StringReader sr = new StringReader(xml);
			o = Unmarshaller.unmarshal(c, sr);
		} catch (MarshalException e) {
			logger.error(e.getMessage());
		} catch (ValidationException e) {
			logger.error(e.getMessage());
		}
		return o;
	}
	
	private static XMLContext getContext() throws IOException, MappingException{
		if (context==null){
			Mapping mapping = new Mapping();
		    InputStream props = XMLUtils.class.getResourceAsStream(MAPPING_FILE);
			InputSource is = new InputSource(props);
			mapping.loadMapping(is);
		
			// initialize and configure XMLContext
			context = new XMLContext();
			context.addMapping(mapping);
		}
		return context;
	}
	
	/**
	 * Utiliza el fichero de mapeo.
	 * @param o
	 * @return
	 */
	public static String marshalFromFile(Object o){
		String xml = null;
		try {
			Marshaller marshaller = getContext().createMarshaller();
			StringWriter sw = new StringWriter();
			marshaller.setWriter(sw);
			marshaller.marshal(o);
			xml = sw.getBuffer().toString();
		} catch (MarshalException e) {
			logger.error(e.getMessage());
		} catch (ValidationException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (MappingException e) {
			logger.error(e.getMessage());
		}
		return xml;
	}
	
	@SuppressWarnings("unchecked")
	private static Object unmarshallFromFile(Class c, String xml){
		Object o = null;
		try {
			<fim_suffix>
		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (MappingException e) {
			logger.error(e.getMessage());
		} catch (MarshalException e) {
			logger.error(e.getMessage());
		} catch (ValidationException e) {
			logger.error(e.getMessage());
		}
		return o;
	}

	/**
	 * M�todo que genera una instancia de CallCenterLlamadas a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a trav�s de variable global
	 * @param xml
	 * @return
	 */
	public static CallCenterLlamadas unmarshallCallCenterLlamadas(String xml){
		CallCenterLlamadas llamadas = null;
		Object o = unmarshallFromFile(CallCenterLlamadas.class, xml);
		if (o!=null){
			llamadas = (CallCenterLlamadas)o;
		}
		return llamadas;
	}

	/**
	 * M�todo que genera una instancia de CallCenterLlamadas a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a trav�s de variable global
	 * @param xml
	 * @return
	 */
	public static LlamadaAlcatel unmarshallLlamadaAlcatel(String xml){
		LlamadaAlcatel llamada = null;
		Object o = unmarshallFromFile(LlamadaAlcatel.class, xml);
		if (o!=null){
			llamada = (LlamadaAlcatel)o;
		}
		return llamada;
	}
	
	/**
	 * M�todo que genera una instancia de CallCenterDatosExtension a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a trav�s de variable global
	 * @param xml
	 * @return
	 */
	public static CallCenterDatosExtension unmarshallCallCenterDatosExtension(String xml){
		CallCenterDatosExtension datosExtension = null;
		Object o = unmarshallFromFile(CallCenterDatosExtension.class, xml);
		if (o!=null){
			datosExtension = (CallCenterDatosExtension)o;
		}
		return datosExtension;
	}
	
	/**
	 * M�todo que genera una instancia de CallCenterDatosLlamada a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a trav�s de variable global
	 * @param xml
	 * @return
	 */
	public static CallCenterDatosLlamada unmarshallCallCenterDatosLlamada(String xml){
		CallCenterDatosLlamada datosLlamada = null;
		Object o = unmarshallFromFile(CallCenterDatosLlamada.class, xml);
		if (o!=null){
			datosLlamada = (CallCenterDatosLlamada)o;
		}
		return datosLlamada;
	}
	
	/**
	 * M�todo que genera una instancia de CallCenterDatosRedireccion a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a trav�s de variable global
	 * @param xml
	 * @return
	 */
	public static CallCenterDatosRedireccion unmarshallCallCenterDatosRedireccion(String xml){
		CallCenterDatosRedireccion datosRedireccion = null;
		Object o = unmarshallFromFile(CallCenterDatosRedireccion.class, xml);
		if (o!=null){
			datosRedireccion = (CallCenterDatosRedireccion)o;
		}
		return datosRedireccion;
	}
	
	/**
	 * M�todo que genera una instancia de CallCenterRespuesta a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a trav�s de variable global
	 * @param xml
	 * @return
	 */
	public static CallCenterRespuesta unmarshallCallCenterRespuesta(String xml){
		CallCenterRespuesta respuesta = null;
		Object o = unmarshallFromFile(CallCenterRespuesta.class, xml);
		if (o!=null){
			respuesta = (CallCenterRespuesta)o;
		}
		return respuesta;
	}

	public static CallCenterDatosNuevaLlamada unmarshallCallCenterDatosNuevaLlamada(
			String xml) {
		CallCenterDatosNuevaLlamada datosNuevaLlamada = null;
		Object o = unmarshallFromFile(CallCenterDatosNuevaLlamada.class, xml);
		if (o!=null){
			datosNuevaLlamada = (CallCenterDatosNuevaLlamada)o;
		}
		return datosNuevaLlamada;
	}
	
	/**
	 * Mï¿½todo que genera una instancia de CallCenterLogRespostaAlcUsuaris a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a travï¿½s de variable global
	 * @param xml
	 * @return
	 */
	public static CallCenterLogRespostaAlcUsuaris unmarshallCallCenterLogRespostaAlcUsuaris(String xml){
		CallCenterLogRespostaAlcUsuaris usuaris = null;
		Object o = unmarshallFromFile(CallCenterLogRespostaAlcUsuaris.class, xml);
		if (o!=null){
			usuaris = (CallCenterLogRespostaAlcUsuaris)o;
		}
		return usuaris;
	}

	/**
	 * Métode que genera una instancia de CallCenterLogPeticioRegistre a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a través de variable global
	 * @param xml
	 * @return
	 */
	public static CallCenterLogPeticioRegistre unmarshallCallCenterLogPeticioRegistre(String xml){
		CallCenterLogPeticioRegistre usuaris = null;
		Object o = unmarshallFromFile(CallCenterLogPeticioRegistre.class, xml);
		if (o!=null){
			usuaris = (CallCenterLogPeticioRegistre)o;
		}
		return usuaris;
	}

	/**
	 * Métode que genera una instancia de CallCenterLogRespostaCallItem a partir de un xml. Utiliza el fichero 
	 * de mapeo indicado a través de variable global
	 * @param xml
	 * @return
	 */
	public static CallCenterLogRespostaCallItem unmarshallCallCenterLogRespostaCallItem(String xml){
		CallCenterLogRespostaCallItem trucades = null;
		Object o = unmarshallFromFile(CallCenterLogRespostaCallItem.class, xml);
		if (o!=null){
			trucades = (CallCenterLogRespostaCallItem)o;
		}
		return trucades;
	}
	
}
<fim_middle>