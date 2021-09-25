package edu.snhu.erik.mattheis.thermostat.db;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ThermostatRepository implements PanacheMongoRepository<Thermostat> {
	@PostConstruct
	void init() {
		mongoCollection().createIndexes(Thermostat.INDEXES);
	}
	
	public Thermostat create(String label, String port) {
		var thermostat = new Thermostat(label, port);
		persist(thermostat);
		return thermostat;
	}
}
