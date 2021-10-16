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

/**
 * represents a document in the thermostat collection in MongoDB
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@MongoEntity(collection = Thermostat.COLLECTION)
@JsonInclude(Include.NON_NULL)
public class Thermostat {

	/**
	 * the name of the collection in MongoDB
	 */
	public static final String COLLECTION = "thermostat";
	
	/**
	 * the indexes to create on the collection
	 * 
	 * <pre>
	 * { label: 1 }, { unique: true }
	 * 
	 * { port: 1 }, { unique: true }
	 * </pre>
	 */
	public static final List<IndexModel> INDEXES = List.of(
			new IndexModel(new BasicDBObject("label", 1), new IndexOptions().unique(true)),
			new IndexModel(new BasicDBObject("port", 1), new IndexOptions().unique(true)));

	public ObjectId id; // _id in MongoDB
	public String label;
	public String port;
	public Instant lastUpdate;
	public Float desiredTemperature;
	public Float ambientTemperature;
	public Boolean heaterOn;
	public Boolean remoteUpdateDisabled;

	/**
	 * factory for creating documents wiht the given label and port
	 * 
	 * @param label a descriptive name for the thermostat
	 * @param port  the system identifier for the port to connect to
	 * @return the document to store in MongoDB
	 */
	public static Thermostat create(String label, String port) {
		Thermostat thermostat = new Thermostat();
		
		thermostat.label = label;
		thermostat.port = port;
		
		return thermostat;
	}
}
