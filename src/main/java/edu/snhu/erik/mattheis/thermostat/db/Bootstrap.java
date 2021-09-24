package edu.snhu.erik.mattheis.thermostat.db;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.mongodb.client.MongoClient;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class Bootstrap {
	@Inject
	MongoClient mongo;

	@Inject
	@ConfigProperty(name = "quarkus.mongodb.database")
	String databaseName;

	void startup(@Observes StartupEvent startup) {
		var db = mongo.getDatabase(databaseName);
		db.getCollection(Thermostat.COLLECTION).createIndexes(Thermostat.INDEXES);
	}
}
