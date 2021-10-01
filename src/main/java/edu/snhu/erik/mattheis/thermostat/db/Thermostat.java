package edu.snhu.erik.mattheis.thermostat.db;

import java.time.Instant;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;

import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection = Thermostat.COLLECTION)
@JsonInclude(Include.NON_NULL)
public class Thermostat {

	public static final String COLLECTION = "thermostat";
	public static final List<IndexModel> INDEXES = List.of(
			new IndexModel(new BasicDBObject("label", 1), new IndexOptions().unique(true)),
			new IndexModel(new BasicDBObject("port", 1), new IndexOptions().unique(true)));

	public ObjectId id;
	public String label;
	public String port;
	public Instant lastUpdate;
	public Float desiredTemperature;
	public Float ambientTemperature;
	public Boolean heaterOn;
	public Boolean remoteUpdateDisabled;

	public static Thermostat create(String label, String port) {
		Thermostat thermostat = new Thermostat();
		
		thermostat.label = label;
		thermostat.port = port;
		
		return thermostat;
	}
}
