package edu.snhu.erik.mattheis.thermostat.db;

import javax.enterprise.context.ApplicationScoped;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ThermostatRepository implements PanacheMongoRepository<Thermostat> {
	public Thermostat findById(String id) {
		return findById(new ObjectId(id));
	}

	public Thermostat create(String label, String port) {
		var thermostat = new Thermostat(label, port);
		persist(thermostat);
		return thermostat;
	}
}
