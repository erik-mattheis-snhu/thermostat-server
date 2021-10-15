package edu.snhu.erik.mattheis.thermostat.comm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fazecast.jSerialComm.SerialPort;

/**
 * represents an available port for communicating with a thermostat
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
public class AvailablePort {

	private final String label;
	private final String port;

	/**
	 * creates an instance with the given label and system identifier
	 * 
	 * @param label the label
	 * @param port  the system identifier
	 */
	@JsonCreator
	public AvailablePort(String label, String port) {
		this.label = label;
		this.port = port;
	}

	/**
	 * gets the label of the port
	 * 
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * gets the system idtifier for the port
	 * 
	 * @return the system identifier
	 */
	public String getPort() {
		return port;
	}

	/**
	 * factory for creating instances from serial ports
	 * 
	 * @param serialPort the serial port to represnt
	 * @return an instance representing the serial port
	 */
	public static AvailablePort of(SerialPort serialPort) {
		return new AvailablePort(serialPort.getPortDescription(), serialPort.getSystemPortName());
	}
}
