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

/**
 * JAX-RS resource for the available-ports endpoint
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@Path("/available-ports")
@ApplicationScoped
public class AvailablePorts {

	@Inject
	ThermostatManager manager;

	/**
	 * gets the list of available ports for connecting to a thermostat
	 * 
	 * <pre>
	 * [
	 *     {
	 *         "label": "XDS110 (03.00.00.16) Embed with CMSIS-DAP",
	 *         "port": "cu.usbmodemE00810104"
	 *     }	
	 * ]
	 * </pre>
	 * 
	 * @return the list of available ports
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<AvailablePort> listAvailablePorts() {
		return manager.getAvailablePorts();
	}
}
