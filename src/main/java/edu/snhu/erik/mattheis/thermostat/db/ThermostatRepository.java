package edu.snhu.erik.mattheis.thermostat.db;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import edu.snhu.erik.mattheis.thermostat.websocket.ThermostatUpdateSocket;
import io.quarkus.mongodb.panache.PanacheMongoRepository;

/**
 * manages access to the thermostat collection in MongoDB
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@ApplicationScoped
public class ThermostatRepository implements PanacheMongoRepository<Thermostat> {

	@Inject
	TemperatureRepository temperatureRepository;
	
	@Inject
	ThermostatUpdateSocket updateSocket;

	/**
	 * ensure the collection has the necessary indexes before use
	 */
	@PostConstruct
	void init() {
		mongoCollection().createIndexes(Thermostat.INDEXES);
	}

	/**
	 * store the latest state of the thermostat
	 */
	@Override
	public void update(Thermostat thermostat) {
		PanacheMongoRepository.super.update(thermostat);
		temperatureRepository.persist(Temperature.ambientOf(thermostat)); // store a snapshot in the tmperature time-series collection
		updateSocket.broadcast(thermostat);                               // broadcast update to any connected front-end websockets
	}
}
