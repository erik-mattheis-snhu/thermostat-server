package edu.snhu.erik.mattheis.thermostat.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateThermostatRequest {

	private final String label;
	private final Float desiredTemperature;

	@JsonCreator
	public UpdateThermostatRequest(@JsonProperty("label") String label, @JsonProperty("desiredTemperature") Float desiredTemperature) {
		this.label = label;
		this.desiredTemperature = desiredTemperature;
	}

	public String getLabel() {
		return label;
	}

	public Float getDesiredTemperature() {
		return desiredTemperature;
	}
}
