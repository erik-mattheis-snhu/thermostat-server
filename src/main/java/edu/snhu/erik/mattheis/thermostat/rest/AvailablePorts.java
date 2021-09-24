package edu.snhu.erik.mattheis.thermostat.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.snhu.erik.mattheis.thermostat.comm.AvailablePort;
import edu.snhu.erik.mattheis.thermostat.comm.ThermostatManager;

@Path("/available-ports")
@ApplicationScoped
public class AvailablePorts {

	@Inject
	ThermostatManager manager;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<AvailablePort> listAvailablePorts() {
		return manager.getAvailablePorts();
	}
}
