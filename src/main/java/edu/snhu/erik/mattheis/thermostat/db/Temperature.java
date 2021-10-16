package edu.snhu.erik.mattheis.thermostat.db;

import static com.mongodb.client.model.TimeSeriesGranularity.MINUTES;
import static edu.snhu.erik.mattheis.thermostat.db.Temperature.COLLECTION;

import java.time.Instant;
import java.util.List;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.TimeSeriesOptions;

import io.quarkus.mongodb.panache.common.MongoEntity;

/**
 * represents a document in the temperature time-series collection in MongoDB
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@MongoEntity(collection = COLLECTION)
public class Temperature {

	/**
	 * the name of the collection in MongoDB
	 */
	public static final String COLLECTION = "temperature";
	
	
	/**
	 * the indexes to create on the collection
	 * 
	 * <pre>
	 * { thermostatId: 1, timestamp: 1 }
	 * </pre>
	 */
	public static final List<IndexModel> INDEXES = List.of(
			new IndexModel(new BasicDBObject("thermostatId", 1).append("timestamp", 1)));
	
	/**
	 * the options to use when creating the time-series collection
	 * 
	 * <pre>
	 * timeseries: {
	 *     timeField: "timestamp",
	 *     metaField: "thermostatId",
	 *     granulatiry: "minutes"
	 * }
	 * </pre>
	 */
	public static final TimeSeriesOptions TIME_SERIES_OPTIONS = new TimeSeriesOptions("timestamp")
			.metaField("thermostatId")
			.granularity(MINUTES);

	public ObjectId id; // _id in MongoDB
	public String thermostatId;
	public Instant timestamp;
	public Float temperature;

	/**
	 * factory for creating documents from themostat state
	 * 
	 * @param thermostat the themostat state to record
	 * @return the document to store in MongoDB
	 */
	public static Temperature ambientOf(Thermostat thermostat) {
		Temperature temperature = new Temperature();
		
		temperature.thermostatId = thermostat.id.toHexString();
		temperature.timestamp = thermostat.lastUpdate;
		temperature.temperature = thermostat.ambientTemperature;
		
		return temperature;
	}
}
