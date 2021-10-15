package edu.snhu.erik.mattheis.thermostat.comm;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

import edu.snhu.erik.mattheis.thermostat.db.Thermostat;
import edu.snhu.erik.mattheis.thermostat.db.ThermostatRepository;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

/**
 * manages configuration of and connections to thermostats
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@ApplicationScoped
public class ThermostatManager {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<ObjectId, ThermostatClient> thermostatClients = new HashMap<>();
	private final Lock clientLock = new ReentrantLock();
	private final Timer timer = new Timer();
	
	private volatile TimerTask poller;

	@Inject
	ThermostatRepository repository;
	
	/**
	 * starts a polling task to maintain connections to configured thermostats
	 * running immediately and every minute thereafter
	 *  
	 * @param startup the Quarkus startup event
	 */
	void onStartup(@Observes StartupEvent startup) {
		poller = new TimerTask() {
			@Override
			public void run() {
				clientLock.lock();
				try {
					repository.listAll().forEach(thermostat -> {           // for all thermostats in the database...
						var client = thermostatClients.get(thermostat.id); // get the associated client
						if (client == null) {                              // if no client exists...
							try {                                          //     create a new one and connect to the thermostat
								var serialPort = SerialPort.getCommPort(thermostat.port);
								client = new ThermostatClient(serialPort, thermostat, repository);
								thermostatClients.put(thermostat.id, client);
								client.connect();
							} catch (Exception e) {
								log.error("problem connecting to thermostat '{}'", thermostat.label, e);
							}
						} else if (!client.isConnected()) {                 // else if the client is disconnected... 
							try {                                           //     try to reconnect to the thermostat
								client.connect();
							} catch (Exception e) {
								log.error("problem reconnecting thermostat '{}'", thermostat.label, e);
							}
						} else if (!upToDate(client.getThermostat())) {     // else if the thermostat is not up to date...
							try {                                           //     request an immediate update
								client.requestUpdate();
							} catch (Exception e) {
								log.error("problem updating thermostat '{}'", thermostat.label, e);
							}
						}
					});
				} finally {
					clientLock.unlock();
				}
			}
		};
		timer.scheduleAtFixedRate(poller, 0, 60000);
	}
	
	/**
	 * cancels the polling task, then disconnects and discards all thermostat clients
	 * 
	 * @param shutdown the Quarkus shutdown event
	 */
	void onShutdown(@Observes ShutdownEvent shutdown) {
		poller.cancel();
		clientLock.lock();
		try {
			thermostatClients.values().forEach(ThermostatClient::disconnect);
			thermostatClients.clear();
		} finally {
			clientLock.unlock();
		}
	}
	
	/**
	 * gets the list of available ports for connecting to a thermostat
	 * 
	 * @return the list of available ports
	 */
	public List<AvailablePort> getAvailablePorts() {
		return Stream.of(SerialPort.getCommPorts())                                // get all the serial ports
		             .filter(port -> !port.getSystemPortName().startsWith("tty.")) // ignore the tty ports on Linux/MacOS
		             .filter(port -> !portHasClient(port.getSystemPortName()))     // ignore ports with existing clients
		             .map(AvailablePort::of)
		             .collect(Collectors.toUnmodifiableList());
	}

	/**
	 * connect to a new thermostat with the designated label and port
	 * 
	 * @param label a descriptive name for the thermostat
	 * @param port  the system identifier for the port to connect to
	 * @return the initial state of the newly connected thermostat
	 * @throws IOException if a failure occurs communicating with the thermostat on the serial port
	 * @throws IllegalArgumentException if label or port is null or blank, or the port is unavailable
	 */
	public Thermostat connectThermostat(String label, String port) throws IOException {
		clientLock.lock();
		try {
			if (label == null || label.isBlank()) {
				throw new IllegalArgumentException("label is required");
			}
			if (port == null || port.isBlank()) {
				throw new IllegalArgumentException("port is required");
			}
			if (portHasClient(port)) {
				throw new IllegalArgumentException("port unavailable");
			}
			var serialPort = SerialPort.getCommPort(port);
			var thermostat = createThermostat(label, port);
			var thermostatClient = new ThermostatClient(serialPort, thermostat, repository);
			thermostatClients.put(thermostat.id, thermostatClient);
			thermostatClient.connect();
			return thermostat;
		} finally {
			clientLock.unlock();
		}
	}

	/**
	 * gets the state of all configured thermostats
	 * 
	 * @return the list of thermostat states 
	 */
	public List<Thermostat> listThermostats() {
		return thermostatClients.values().stream()
				.map(ThermostatClient::getThermostat)
				.collect(Collectors.toUnmodifiableList());
	}

	/**
	 * gets the state of a configured thermostat with the given id
	 * 
	 * @param id the id of the thermostat to get
	 * @return the state of the matching thermostat or {@link Optional#empty()} if no thermostat matches the id
	 */
	public Optional<Thermostat> getThermostat(ObjectId id) {
		return Optional.ofNullable(thermostatClients.get(id)).map(ThermostatClient::getThermostat);
	}

	/**
	 * sets the label of the thermostat with the given id
	 * 
	 * @param id the id of the thermostat to update
	 * @param label a descriptive name for the thermostat
	 * @return the updated state of the matching thermostat or {@link Optional#empty()} if no thermostat matches the id
	 */
	public Optional<Thermostat> setThermostatLabel(ObjectId id, String label) {
		var client = thermostatClients.get(id);
		if (client == null) {
			return Optional.empty();
		}
		var thermostat = client.getThermostat();
		thermostat.label = label;
		repository.update(thermostat);
		return Optional.of(thermostat);
	}

	/**
	 * sets the desired temperature of the thermostat with the given id
	 * 
	 * @param id the id of the thermostat to update
	 * @param desiredTemperature the desired temperature to set on the thermostat
	 * @return the updated state of the matching thermostat or {@link Optional#empty()} if no thermostat matches the id
	 * @throws IOException if a failure occurs communicating with the thermostat on the serial port
	 * @throws IllegalStateException if the client is not connected or remote updates are disabled on the thermostat
	 * @throws TimeoutException if an update is not received from the thermostat within 5 seconds
	 * @throws InterruptedException if the thread is interrupted while waiting for an update
	 */
	public Optional<Thermostat> setThermostatDesiredTemperature(ObjectId id, float desiredTemperature)
			throws IOException, TimeoutException, InterruptedException {
		var client = thermostatClients.get(id);
		if (client == null) {
			return Optional.empty();
		}
		return Optional.of(client.setDesiredTemperature(desiredTemperature));
	}

	/**
	 * disconnects from the thermostat with the given id and discards the configuration
	 * 
	 * @param id the id of the thermostat to disconnect from
	 * @return {@code true} if the thermostat configuation was successfully discarded 
	 */
	public boolean disconnectThermostat(ObjectId id) {
		clientLock.lock();
		try {
			var client = thermostatClients.remove(id);
			if (client == null) {
				return false;
			}
			repository.delete(client.getThermostat());
			client.disconnect();
			return true;
		} finally {
			clientLock.unlock();
		}
	}

	private Thermostat createThermostat(String label, String port) {
		var thermostat = Thermostat.create(label, port);
		repository.persist(thermostat);
		return thermostat;
	}
	
	private boolean portHasClient(String port) {
		return thermostatClients.values().stream().filter(client -> port.equals(client.getThermostat().port)).findAny()
				.isPresent();
	}

	private static boolean upToDate(Thermostat thermostat) {
		return thermostat.lastUpdate.plus(1, MINUTES).isAfter(now());
	}
}
