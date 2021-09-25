package edu.snhu.erik.mattheis.thermostat.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateThermostatRequest {

	private final String label;
	private final String port;

	@JsonCreator
	public CreateThermostatRequest(@JsonProperty("label") String label, @JsonProperty("port") String port) {
		this.label = label;
		this.port = port;
	}

	public String getLabel() {
		return label;
	}

	public String getPort() {
		return port;
	}
}
