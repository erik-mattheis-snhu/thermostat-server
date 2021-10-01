package edu.snhu.erik.mattheis.thermostat.db;

import static com.mongodb.client.model.Accumulators.avg;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Projections.computed;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class TemperatureRepository implements PanacheMongoRepository<Temperature> {

	@PostConstruct
	void init() {
		MongoDatabase db = mongoDatabase();
		if (!contains(db.listCollectionNames(), Temperature.COLLECTION)) {
			db.createCollection(Temperature.COLLECTION, new CreateCollectionOptions()
					.timeSeriesOptions(Temperature.TIME_SERIES_OPTIONS).expireAfter(365, TimeUnit.DAYS));
		}
		mongoCollection().createIndexes(Temperature.INDEXES);
	}

	public TemperatureHistory getTemperatureHistory(String thermostatId, Instant from, Instant to) {
		var unit = "minute";
		var binSize = 15;
		var match = match(and(eq("thermostatId", thermostatId), gte("timestamp", from), lt("timestamp", to)));
		var project = project(fields(excludeId(), include("temperature"), computed("timestamp", computed("$dateAdd",
				new BasicDBObject("startDate", computed("$dateTrunc",
						new BasicDBObject("date", "$timestamp").append("unit", unit).append("binSize", binSize)))
								.append("unit", unit).append("amount", binSize)))));
		var group = group("$timestamp", avg("temperature", "$temperature"));
		var sort = sort(include("_id"));
		var documents = mongoCollection().aggregate(List.of(match, project, group, sort), Document.class);
		return TemperatureHistory.of(documents, "_id", "temperature");
	}

	private static <T> boolean contains(Iterable<T> iterable, T match) {
		for (T t : iterable) {
			if (Objects.equals(t, match)) {
				return true;
			}
		}
		return false;
	}
}
