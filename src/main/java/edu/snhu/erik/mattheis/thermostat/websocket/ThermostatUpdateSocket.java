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

@ServerEndpoint(value = "/api/thermostats/{id}/updates", encoders = JsonEncoder.class)
@ApplicationScoped
public class ThermostatUpdateSocket {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<String, Set<Session>> thermostatSessions = new ConcurrentHashMap<>();

	@OnOpen
	public void onOpen(Session session, @PathParam("id") String thermostatId) {
		thermostatSessions.computeIfAbsent(thermostatId, key -> ConcurrentHashMap.newKeySet()).add(session);
		log.info("session {} for thermostat {} opened", session.getId(), thermostatId);
	}

	@OnClose
	public void onClose(Session session, @PathParam("id") String thermostatId) {
		thermostatSessions.values().forEach(sessions -> sessions.remove(session));
		log.info("session {} for thermostat {} closed", session.getId(), thermostatId);
	}

	@OnError
	public void onError(Session session, @PathParam("id") String thermostatId, Throwable throwable) {
		thermostatSessions.values().forEach(sessions -> sessions.remove(session));
		log.error("session {} for thermostat {} errored", session.getId(), thermostatId, throwable);
	}

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
