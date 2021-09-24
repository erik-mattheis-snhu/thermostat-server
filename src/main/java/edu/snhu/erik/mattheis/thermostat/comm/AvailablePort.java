package edu.snhu.erik.mattheis.thermostat.comm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fazecast.jSerialComm.SerialPort;

public class AvailablePort {
	private final String label;
	private final String port;

	@JsonCreator
	public AvailablePort(String label, String port) {
		this.label = label;
		this.port = port;
	}

	public String getLabel() {
		return label;
	}

	public String getPort() {
		return port;
	}

	public static AvailablePort of(SerialPort serialPort) {
		return new AvailablePort(serialPort.getPortDescription(), serialPort.getSystemPortName());
	}
}
