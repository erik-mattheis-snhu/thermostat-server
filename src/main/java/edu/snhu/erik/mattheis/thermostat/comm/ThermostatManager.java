package edu.snhu.erik.mattheis.thermostat.comm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.fazecast.jSerialComm.SerialPort;

@ApplicationScoped
public class ThermostatManager {
	private final Map<String, SerialPort> connectedPorts = new ConcurrentHashMap<>();
	
	public List<AvailablePort> getAvailablePorts() {
		return Stream.of(SerialPort.getCommPorts())
				.filter(port -> !connectedPorts.containsKey(port.getSystemPortName()))
				.map(AvailablePort::of)
				.collect(Collectors.toUnmodifiableList());
	}
}
