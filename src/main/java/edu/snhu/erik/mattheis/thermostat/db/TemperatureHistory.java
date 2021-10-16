package edu.snhu.erik.mattheis.thermostat.db;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER_INT;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * presents the result of a time-series query for temperature in
 * a format directly suitable for graphing on the front-end
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
public class TemperatureHistory {

	private final List<Instant> timestamps;
	private final List<Double> temperatures;

	private TemperatureHistory(List<Instant> timestamps, List<Double> temperatures) {
		this.timestamps = List.copyOf(timestamps);
		this.temperatures = List.copyOf(temperatures);
	}

	/**
	 * gets the list of timestamps from all the query results
	 * 
	 * @return the list of timestamps
	 */
	@JsonFormat(shape = NUMBER_INT, without = WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
	public List<Instant> getTimestamps() {
		return timestamps;
	}

	/**
	 * gets the list of temperatures from all the query results
	 * @return
	 */
	public List<Double> getTemperatures() {
		return temperatures;
	}

	/**
	 * factory for creating instances from a MongoDB query
	 * 
	 * @param documents the documents returned by the query 
	 * @param timestampKey the name of the field containing the timestamp in each document
	 * @param temperatureKey the name of the field containing the temperature in each document
	 * @return a new instance with the timestamps and temperatures extract from the query results
	 */
	public static TemperatureHistory of(Iterable<Document> documents, String timestampKey, String temperatureKey) {
		List<Instant> timestamps = new ArrayList<>();
		List<Double> temperatures = new ArrayList<>();
		for (Document document : documents) {
			timestamps.add(document.getDate(timestampKey).toInstant());
			temperatures.add(document.getDouble(temperatureKey));
		}
		return new TemperatureHistory(timestamps, temperatures);
	}
}
