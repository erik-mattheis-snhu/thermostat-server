package edu.snhu.erik.mattheis.thermostat.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import edu.snhu.erik.mattheis.thermostat.db.Thermostat;
import edu.snhu.erik.mattheis.thermostat.db.ThermostatRepository;

@Path("/thermostats")
@ApplicationScoped
public class Thermostats {

	@Inject
	ThermostatRepository repository;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Thermostat> listThermostats() {
		return repository.listAll();
	}
}
