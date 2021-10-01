package edu.snhu.erik.mattheis.thermostat.db;

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.NUMBER_INT;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.fasterxml.jackson.annotation.JsonFormat;

public class TemperatureHistory {

	private final List<Instant> timestamps;
	private final List<Double> temperatures;

	private TemperatureHistory(List<Instant> timestamps, List<Double> temperatures) {
		this.timestamps = List.copyOf(timestamps);
		this.temperatures = List.copyOf(temperatures);
	}

	@JsonFormat(shape = NUMBER_INT, without = WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
	public List<Instant> getTimestamps() {
		return timestamps;
	}

	public List<Double> getTemperatures() {
		return temperatures;
	}

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
