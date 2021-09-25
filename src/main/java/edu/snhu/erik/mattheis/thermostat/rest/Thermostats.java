package edu.snhu.erik.mattheis.thermostat.rest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.bson.types.ObjectId;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import com.mongodb.MongoWriteException;

import edu.snhu.erik.mattheis.thermostat.comm.ThermostatManager;
import edu.snhu.erik.mattheis.thermostat.db.Thermostat;

@Path("/thermostats")
@ApplicationScoped
public class Thermostats {

	@Inject
	ThermostatManager manager;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Thermostat> listThermostats() {
		return manager.listThermostats();
	}

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

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Thermostat getThermostat(@PathParam("id") ObjectId id) {
		return manager.getThermostat(id).orElseThrow(NotFoundException::new);
	}

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
				throw new ClientErrorException(Status.GATEWAY_TIMEOUT, e);
			} catch (IOException | InterruptedException e) {
				throw new InternalServerErrorException(e);
			}
		}
		return getThermostat(id);
	}

	@DELETE
	@Path("/{id}")
	public void deleteThermostat(@PathParam("id") ObjectId id) {
		if (!manager.disconnectThermostat(id)) {
			throw new NotFoundException();
		}
	}
}
