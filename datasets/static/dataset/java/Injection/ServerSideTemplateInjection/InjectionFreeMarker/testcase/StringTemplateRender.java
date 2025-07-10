<filename>iMatrix/imatrix-oracle-5.2.0.RC/src/main/java/com/norteksoft/product/util/StringTemplateRender.java<fim_prefix>

package com.norteksoft.product.util;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

public class StringTemplateRender implements TemplateRender {
	private static Configuration config = null;

	public static StringTemplateRender getInstance() {
		return new StringTemplateRender();
	}

	public StringTemplateRender() {
		if (config == null) {
			config = new Configuration();
			config.setTemplateLoader(new StringTemplateLoader());

			try {
				config.setSetting("datetime_format", "yyyy-MM-dd HH:mm:ss");
				config.setLocale(Locale.CHINA);

			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}
	}

	@SuppressWarnings("unchecked")
	public String render(Object dataModel, String text) throws Exception {
		String ret = null;
		if ((dataModel instanceof Map) == false) {
			throw new IllegalArgumentException("参数dataModel必须为一个Map对象");
		}
		BufferedReader reader=null;
		StringWriter stringWriter=null;
		BufferedWriter writer=null;
		try { 
			reader= new BufferedReader(new StringReader(text));
			
			<fim_suffix>

			Template template = new Template(null, reader, config, "UTF-8");
			SimpleHash root = new SimpleHash();
			root.putAll((Map) dataModel);
			stringWriter= new StringWriter();
			writer = new BufferedWriter(stringWriter);
			template.process(root, writer);
			writer.flush();
			ret = stringWriter.toString();

		} catch (Exception ex) {
			ex.printStackTrace();
			ret = null;
		}finally{
			reader.close();
			stringWriter.close();
			writer.close();
		}
		return ret;
	}
}
<fim_middle>