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
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import edu.snhu.erik.mattheis.thermostat.db.Thermostat;
import edu.snhu.erik.mattheis.thermostat.db.ThermostatRepository;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ThermostatManager {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final Map<ObjectId, ThermostatClient> thermostatClients = new HashMap<>();
	private final Lock clientLock = new ReentrantLock();
	private final Timer timer = new Timer();

	@Inject
	ThermostatRepository repository;
	void onStartup(@Observes StartupEvent startup) {
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				clientLock.lock();
				try {
					repository.listAll().forEach(thermostat -> {
						var client = thermostatClients.get(thermostat.id);
						if (client == null) {
							try {
								var serialPort = SerialPort.getCommPort(thermostat.port);
								client = new ThermostatClient(serialPort, thermostat, repository);
								thermostatClients.put(thermostat.id, client);
								client.connect();
							} catch (SerialPortInvalidPortException | IOException e) {
								log.error("problem connecting to thermostat '{}'", thermostat.label, e);
							}
						} else if (!client.isConnected()) {
							try {
								client.connect();
							} catch (IOException e) {
								log.error("problem reconnecting thermostat '{}'", thermostat.label, e);
							}
						} else if (client.getThermostat().lastUpdate.plus(1, MINUTES).isAfter(now())) {
							try {
								client.requestUpdate();
							} catch (IOException e) {
								log.error("problem updating thermostat '{}'", thermostat.label, e);
							}
						}
					});
				} finally {
					clientLock.unlock();
				}
			}
		}, 0, 60000);
	}
	
	public List<AvailablePort> getAvailablePorts() {
		return Stream.of(SerialPort.getCommPorts()).filter(port -> !port.getSystemPortName().startsWith("tty."))
				.filter(port -> !portHasClient(port.getSystemPortName())).map(AvailablePort::of)
				.collect(Collectors.toUnmodifiableList());
	}

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
			var thermostat = repository.create(label, port);
			var thermostatClient = new ThermostatClient(serialPort, thermostat, repository);
			thermostatClients.put(thermostat.id, thermostatClient);
			thermostatClient.connect();
			return thermostat;
		} finally {
			clientLock.unlock();
		}
	}

	public List<Thermostat> listThermostats() {
		return thermostatClients.values().stream()
				.map(ThermostatClient::getThermostat)
				.collect(Collectors.toUnmodifiableList());
	}

	public Optional<Thermostat> getThermostat(ObjectId id) {
		return Optional.ofNullable(thermostatClients.get(id)).map(ThermostatClient::getThermostat);
	}

	public Optional<Thermostat> setThermostatLabel(ObjectId id, String label) throws IOException {
		var client = thermostatClients.get(id);
		if (client == null) {
			return Optional.empty();
		}
		var thermostat = client.getThermostat();
		thermostat.label = label;
		client.requestUpdate();
		return Optional.of(thermostat);
	}

	public Optional<Thermostat> setThermostatDesiredTemperature(ObjectId id, float desiredTemperature)
			throws IOException, TimeoutException, InterruptedException {
		var client = thermostatClients.get(id);
		if (client == null) {
			return Optional.empty();
		}
		return Optional.of(client.setDesiredTemperature(desiredTemperature));
	}

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

	private boolean portHasClient(String port) {
		return thermostatClients.values().stream().filter(client -> port.equals(client.getThermostat().port)).findAny()
				.isPresent();
	}
}
