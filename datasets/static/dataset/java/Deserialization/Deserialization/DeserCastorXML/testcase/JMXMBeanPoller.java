<filename>JMXMBeanPoller.java<fim_prefix>

package com.splunk.javaagent.jmx;

import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.List;

import javax.management.MBeanServerConnection;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Unmarshaller;
import org.xml.sax.InputSource;

import com.splunk.javaagent.jmx.config.Formatter;
import com.splunk.javaagent.jmx.config.JMXPoller;
import com.splunk.javaagent.jmx.config.JMXServer;
import com.splunk.javaagent.jmx.config.Transport;

public class JMXMBeanPoller {

	// private static Logger logger = LoggerFactory
	// .getLogger(JMXMBeanPoller.class);

	private JMXPoller config;
	private Formatter formatter;
	private Transport transport;
	private MBeanServerConnection serverConnection;
	boolean registerNotifications = true;

	public JMXMBeanPoller(String configFile) {

		try {
			this.config = JMXMBeanPoller.loadConfig(configFile);
			this.config.normalizeClusters();
			this.formatter = config.getFormatter();
			if (this.formatter == null) {
				this.formatter = new Formatter();// default
			}
			this.transport = config.getTransport();
			if (this.transport == null) {
				this.transport = new Transport();// default
			}
			connect();
		} catch (Throwable e) {

		}

	}

	/**
	 * Connect to the local JMX Server
	 * 
	 * @throws Exception
	 */
	private void connect() throws Exception {

		this.serverConnection = ManagementFactory.getPlatformMBeanServer();

	}

	public void execute() {

		// logger.info("Starting JMX Poller");
		try {

			if (this.config != null) {
				// get list of JMX Servers and process in their own thread.
				List<JMXServer> servers = this.config.normalizeMultiPIDs();
				if (servers != null) {

					for (JMXServer server : servers) {
						new ProcessServerThread(server,
								this.formatter.getFormatterInstance(),
								this.transport.getTransportInstance(),
								this.registerNotifications,
								this.serverConnection).start();
					}
					// we only want to register a notification listener on the
					// first iteration
					this.registerNotifications = false;
				} else {
					// logger.error("No JMX servers have been specified");
				}
			} else {
				// logger.error("The root config object(JMXPoller) failed to initialize");
			}
		} catch (Exception e) {

			// logger.error("Error : " + e.getMessage());
			// System.exit(1);
		}
	}

	/**
	 * Parse the config XML into Java POJOs and validate against XSD
	 * 
	 * @param configFileName
	 * @return The configuration POJO root
	 * @throws Exception
	 */
	private static JMXPoller loadConfig(String configFileName) throws Exception {

		URL file = JMXMBeanPoller.class.getResource("/" + configFileName);

		if (file == null) {
			throw new Exception("The config file " + configFileName
					+ " does not exist");
		}
		// xsd validation
		InputSource inputSource = new InputSource(file.openStream());
		SchemaValidator validator = new SchemaValidator();
		validator.validateSchema(inputSource);

		// use CASTOR to parse XML into Java POJOs
		Mapping mapping = new Mapping();

		URL mappingURL = JMXMBeanPoller.class
				.getResource("/com/splunk/javaagent/jmx/mapping.xml");
        <fim_suffix>

		// for some reason the xsd validator closes the file stream, so re-open
		inputSource = new InputSource(file.openStream());
		JMXPoller poller = (JMXPoller) unmar.unmarshal(inputSource);

		return poller;

	}

}
<fim_middle>