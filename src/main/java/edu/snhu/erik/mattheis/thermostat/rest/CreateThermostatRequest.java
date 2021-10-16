package edu.snhu.erik.mattheis.thermostat.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * represents a request to create a new thermostat configuration
 * 
 * <pre>
 * {
 *     "label": "Prototype Board",
 *     "port": "cu.usbmodemE00810101"
 * }
 * </pre>
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
public class CreateThermostatRequest {

	private final String label;
	private final String port;

	/**
	 * creates a new instance with the given label and port
	 * 
	 * @param label a descriptive name for the thermostat
	 * @param port  the system identifier for the port to connect to
	 */
	@JsonCreator
	public CreateThermostatRequest(@JsonProperty("label") String label, @JsonProperty("port") String port) {
		this.label = label;
		this.port = port;
	}

	/**
	 * returns the descriptive name for the thermostat
	 * 
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * returns the system identifier for the port to connect to
	 * 
	 * @return the port
	 */
	public String getPort() {
		return port;
	}
}
