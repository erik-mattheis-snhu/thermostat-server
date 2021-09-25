package edu.snhu.erik.mattheis.thermostat.db;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class ThermostatRepository implements PanacheMongoRepository<Thermostat> {
	@Inject
	MongoClient mongo;

	@Inject
	@ConfigProperty(name = "quarkus.mongodb.database")
	String databaseName;

	@PostConstruct
	void init() {
		mongo.getDatabase(databaseName)
			.getCollection(Thermostat.COLLECTION)
			.createIndexes(Thermostat.INDEXES);
	}
	
	public Thermostat create(String label, String port) {
		var thermostat = new Thermostat(label, port);
		persist(thermostat);
		return thermostat;
	}
}
