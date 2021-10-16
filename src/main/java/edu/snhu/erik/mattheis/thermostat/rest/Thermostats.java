package edu.snhu.erik.mattheis.thermostat.rest;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.bson.types.ObjectId;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import com.mongodb.MongoWriteException;

import edu.snhu.erik.mattheis.thermostat.comm.ThermostatManager;
import edu.snhu.erik.mattheis.thermostat.db.TemperatureHistory;
import edu.snhu.erik.mattheis.thermostat.db.TemperatureRepository;
import edu.snhu.erik.mattheis.thermostat.db.Thermostat;

/**
 * JAX-RS resource for the thermostats endpoint
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@Path("/thermostats")
@ApplicationScoped
public class Thermostats {

	@Inject
	ThermostatManager manager;
	
	@Inject
	TemperatureRepository temperatureRepository;

	/**
	 * gets the state of all configured thermostats
	 * 
	 * <pre>
	 * [
	 *     {
	 *         "id": "614e59d4fb04a00ca2b7a984",
	 *         "label": "Prototype Board",
	 *         "port": "cu.usbmodemE00810101",
	 *         "lastUpdate": "2021-10-16T01:20:27.747Z",
	 *         "desiredTemperature": 20.0,
	 *         "ambientTemperature": 25.34375,
	 *         "heaterOn": false,
	 *         "remoteUpdateDisabled": false
	 *     }
	 * ]
	 * </pre>
	 * 
	 * @return the list of thermostat states 
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Thermostat> listThermostats() {
		return manager.listThermostats();
	}

	/**
	 * creates a new thermostat configuration using the given request,
	 * redirects to the thermostat status resources on success
	 * 
	 * @param uriInfo context information supplied by the container - used to generate the redirect location
	 * @param request the create request
	 * @return a response indicating sucess or failure
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createThermostat(@Context UriInfo uriInfo, CreateThermostatRequest request) {
		try {
			var thermostat = manager.connectThermostat(request.getLabel(), request.getPort());
			var location = uriInfo.getAbsolutePathBuilder()
					.path(Thermostats.class, "getThermostat")
					.build(thermostat.id);
			return Response.seeOther(location).build();
		} catch (MongoWriteException e) {
			return Response.status(Status.CONFLICT).entity(e.getError().getMessage()).build();
		} catch (IllegalArgumentException | SerialPortInvalidPortException e) {
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch (IOException e) {
			return Response.serverError().entity(e.getMessage()).build();
		}
	}

	/**
	 * gets the state of a configured thermostat with the given id
	 * 
	 * <pre>
	 * {
	 *     "id": "614e59d4fb04a00ca2b7a984",
	 *     "label": "Prototype Board",
	 *     "port": "cu.usbmodemE00810101",
	 *     "lastUpdate": "2021-10-16T01:30:09.642500Z",
	 *     "desiredTemperature": 20.0,
	 *     "ambientTemperature": 25.0625,
	 *     "heaterOn": false,
	 *     "remoteUpdateDisabled": false
	 * }
	 * </pre>
	 * 
	 * @param id the id of the thermostat to get
	 * @return the state of the matching thermostat
	 * @throws NotFoundException if no thermostat was found with the given ID
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Thermostat getThermostat(@PathParam("id") ObjectId id) {
		return manager.getThermostat(id).orElseThrow(NotFoundException::new);
	}

	/**
	 * updates the thermostat with the given id using the given request,
	 * returns the latest state when successful
	 * 
	 * <pre>
	 * {
	 *     "id": "614e59d4fb04a00ca2b7a984",
	 *     "label": "Prototype Board",
	 *     "port": "cu.usbmodemE00810101",
	 *     "lastUpdate": "2021-10-16T01:36:13.239769Z",
	 *     "desiredTemperature": 22.5,
	 *     "ambientTemperature": 25.25,
	 *     "heaterOn": false,
	 *     "remoteUpdateDisabled": false
	 * }
	 * </pre>
	 * 
	 * @param id the id of the thermostat to update
	 * @param request the updates to perform
	 * @return the updated state of the thermostat
	 * @throws NotFoundException if no thermostat was found with the given ID
	 */
	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Thermostat updateThermostat(@PathParam("id") ObjectId id, UpdateThermostatRequest request) {
		if (request.getDesiredTemperature() != null) {
			try {
				return manager.setThermostatDesiredTemperature(id, request.getDesiredTemperature())
						.orElseThrow(NotFoundException::new);
			} catch (IllegalStateException e) {
				throw new ForbiddenException(e);
			} catch (TimeoutException e) {
				throw new ServerErrorException(Status.GATEWAY_TIMEOUT, e);
			} catch (IOException | InterruptedException e) {
				throw new InternalServerErrorException(e);
			}
		}
		return getThermostat(id);
	}

	/**
	 * disconnects from the thermostat with the given id and discards the configuration
	 * 
	 * @param id the id of the thermostat to disconnect from
	 * @throws NotFoundException if no thermostat was found with the given ID
	 */
	@DELETE
	@Path("/{id}")
	public void deleteThermostat(@PathParam("id") ObjectId id) {
		if (!manager.disconnectThermostat(id)) {
			throw new NotFoundException();
		}
	}
	
	/**
	 * gets the temperature history for the given thermostat
	 * using average temperatures over 15 minute intervals
	 * 
	 * <pre>
	 * {
	 *     "timestamps": [
	 *         1634337900000,
	 *         1634338800000,
	 *         1634339700000,
	 *         1634340600000,
	 *         1634341500000
	 *     ],
	 *     "temperatures": [
	 *         25.09375,
	 *         25.234375,
	 *         25.1875,25.
	 *         12215909090909,
	 *         25.069196428571427
	 *     ]
	 * }
	 * </pre>
	 * 
	 * @param id the ID of the thermostat to get the temperature history for
	 * @param from the start fo the time period to report on
	 * @param to the end of the time period to report on
	 * @return the temperature history
	 */
	@GET
	@Path("/{id}/temperature/history")
	@Produces(MediaType.APPLICATION_JSON)
	public TemperatureHistory getThermostatTemperatureHistory(@PathParam("id") String id, @QueryParam("from") Instant from, @QueryParam("to") Instant to) {
		return temperatureRepository.getTemperatureHistory(id, from, to);
	}
}
