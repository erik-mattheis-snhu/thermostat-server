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

@MongoEntity(collection = COLLECTION)
public class Temperature {

	public static final String COLLECTION = "temperature";
	public static final List<IndexModel> INDEXES = List.of(
			new IndexModel(new BasicDBObject("thermostatId", 1)
					.append("timestamp", 1)));
	public static final TimeSeriesOptions TIME_SERIES_OPTIONS = new TimeSeriesOptions("timestamp")
			.metaField("thermostatId")
			.granularity(MINUTES);

	public ObjectId id;
	public String thermostatId;
	public Instant timestamp;
	public Float temperature;

	public static Temperature ambientOf(Thermostat thermostat) {
		Temperature temperature = new Temperature();
		
		temperature.thermostatId = thermostat.id.toHexString();
		temperature.timestamp = thermostat.lastUpdate;
		temperature.temperature = thermostat.ambientTemperature;
		
		return temperature;
	}
}
