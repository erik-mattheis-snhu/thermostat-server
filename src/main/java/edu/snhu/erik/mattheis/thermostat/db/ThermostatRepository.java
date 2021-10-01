package edu.snhu.erik.mattheis.thermostat.db;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import edu.snhu.erik.mattheis.thermostat.websocket.ThermostatUpdateSocket;
import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ThermostatRepository implements PanacheMongoRepository<Thermostat> {

	@Inject
	TemperatureRepository temperatureRepository;
	
	@Inject
	ThermostatUpdateSocket updateSocket;

	@PostConstruct
	void init() {
		mongoCollection().createIndexes(Thermostat.INDEXES);
	}

	@Override
	public void update(Thermostat thermostat) {
		PanacheMongoRepository.super.update(thermostat);
		temperatureRepository.persist(Temperature.ambientOf(thermostat));
		updateSocket.broadcast(thermostat);
	}
}
