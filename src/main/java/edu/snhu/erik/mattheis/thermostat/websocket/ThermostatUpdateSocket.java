package edu.snhu.erik.mattheis.thermostat.websocket;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.snhu.erik.mattheis.thermostat.db.Thermostat;

/**
 * a websocket server endpoint for providing thermostat updates
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@ServerEndpoint(value = "/api/thermostats/{id}/updates", encoders = JsonEncoder.class)
@ApplicationScoped
public class ThermostatUpdateSocket {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<String, Set<Session>> thermostatSessions = new ConcurrentHashMap<>();

	/**
	 * stores the session in a map by thermostat ID
	 * 
	 * @param session the opened websocket session
	 * @param thermostatId the ID of the thermostat to provide updates for
	 */
	@OnOpen
	public void onOpen(Session session, @PathParam("id") String thermostatId) {
		thermostatSessions.computeIfAbsent(thermostatId, key -> ConcurrentHashMap.newKeySet()).add(session);
		log.info("session {} for thermostat {} opened", session.getId(), thermostatId);
	}

	/**
	 * removes the session from the map
	 * 
	 * @param session the closed session
	 * @param thermostatId the ID of the thermostat the session was listening to
	 */
	@OnClose
	public void onClose(Session session, @PathParam("id") String thermostatId) {
		thermostatSessions.values().forEach(sessions -> sessions.remove(session));
		log.info("session {} for thermostat {} closed", session.getId(), thermostatId);
	}

	/**
	 * removes the session from the map
	 * 
	 * @param session the closed session
	 * @param thermostatId the ID of the thermostat the session was listening to
	 */
	@OnError
	public void onError(Session session, @PathParam("id") String thermostatId, Throwable throwable) {
		thermostatSessions.values().forEach(sessions -> sessions.remove(session));
		log.error("session {} for thermostat {} errored", session.getId(), thermostatId, throwable);
	}

	/**
	 * sends the provided thermostat state as a JSON payload to all
	 * sessions linked to the correspoinding ID
	 * 
	 * @param thermostat the thermostat state to send
	 */
	public void broadcast(Thermostat thermostat) {
		var thermostatId = thermostat.id.toHexString();
		Optional.ofNullable(thermostatSessions.get(thermostatId)).stream().flatMap(Set::stream)
				.forEach(session -> session.getAsyncRemote().sendObject(thermostat, result -> {
					var sessionId = session.getId();
					if (result.isOK()) {
						log.info("updated session {} for thermostat {}", sessionId, thermostatId);
					} else {
						var exception = result.getException();
						log.error("failed to update session {} for thermostat {}", sessionId, thermostatId, exception);
					}
				}));
	}
}
