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

/**
 * manages access to the temperature time-series collection in MongoDB
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@ApplicationScoped
public class TemperatureRepository implements PanacheMongoRepository<Temperature> {

	/**
	 * ensure the collection is properly created and has the necessary indexes before use
	 */
	@PostConstruct
	void init() {
		MongoDatabase db = mongoDatabase();
		if (!contains(db.listCollectionNames(), Temperature.COLLECTION)) {             // if the collection doesn't exist...
			db.createCollection(Temperature.COLLECTION, new CreateCollectionOptions()  //     create it
					.timeSeriesOptions(Temperature.TIME_SERIES_OPTIONS)                //     using the appropriate options
					.expireAfter(365, TimeUnit.DAYS));                                 //     with documents expiring after one year
		}
		mongoCollection().createIndexes(Temperature.INDEXES);                          // ensure the indexes are created
	}

	/**
	 * queries aggregated temperature history from the time-series collection in MongoDB
	 * using average temperatures over 15 minute intervals
	 * 
	 * @param thermostatId the ID of the thermostat to report on
	 * @param from the start fo the time period to report on
	 * @param to the end of the time period to report on
	 * @return the aggregated results in a format suitable for graphing on the front-end 
	 */
	public TemperatureHistory getTemperatureHistory(String thermostatId, Instant from, Instant to) {
		var unit = "minute";
		var binSize = 15;
		/*
		 * match all documents for the given thermostatId within the time period
		 * 
		 * { $match: { $and: [ { $eq:  [ "thermostatId", thermostatId ] },
		 *                     { $gte: [ "timestamp",    from         ] },
		 *                     { $lt:  [ "timestamp",    to           ] } ] } }
		 */
		var match = match(and(eq("thermostatId", thermostatId), gte("timestamp", from), lt("timestamp", to)));
		/*
		 * exclude the _id field, include termparature and round timestamp up to the next 15 minute boundary
		 * 
		 * { $project: { _id: 0,
		 *               temperature: 1,
		 *               timestamp: {
		 *                   $dateAdd: {
		 *                       startDate: {
		 *                           $dateTrunc: {
		 *                               date: "$timestamp",
		 *                               unit: "minute",
		 *                               binSize: 15
		 *                           },
		 *                       },
		 *                       unit: "minute",
		 *                       amount: 15
		 *                   }
		 *               } } }
		 */
		var project = project(fields(excludeId(), include("temperature"), computed("timestamp", computed("$dateAdd",
				new BasicDBObject("startDate", computed("$dateTrunc",
						new BasicDBObject("date", "$timestamp").append("unit", unit).append("binSize", binSize)))
								.append("unit", unit).append("amount", binSize)))));
		/*
		 * average all temperatures from the same 15 minute interval
		 * 
		 * { $group: { _id: "$timestamp",
		 *             temperature: { $avg: "$temperature" } } }
		 */
		var group = group("$timestamp", avg("temperature", "$temperature"));
		/*
		 * sort by _id (timestamp)
		 * 
		 * { $sort: { _id: 1 } }
		 */
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
