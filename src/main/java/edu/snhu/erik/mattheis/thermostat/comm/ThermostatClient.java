package edu.snhu.erik.mattheis.thermostat.comm;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

import edu.snhu.erik.mattheis.thermostat.db.Thermostat;
import edu.snhu.erik.mattheis.thermostat.db.ThermostatRepository;

/**
 * manages communication with a thermostat
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
public class ThermostatClient {
	
	private static final byte[] LF = { 0x0A }; // ASCII line-feed character

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ThermostatRepository repository;
	private final Thermostat thermostat;
	private final SerialPort serialPort;
	
	/*
	 * anonymous listener implementation for receiving messages from the thermostst
	 * 
	 * message are delimited by ASCII line-feeds and have the following format:
	 * 
	 *     D:20.000000,A:25.187500,H:0,L:0
	 *
	 * where the fields are as follows:
	 *
	 *     D: desired temperature (degrees C)
	 *     A: ambient temperature (degrees C)
	 *     H: heater state (0 = off, 1 = on)
	 *     L: remote lock (0 = off, 1 = on)
	 */
	private final SerialPortMessageListener listener = new SerialPortMessageListener() {
		public void serialEvent(SerialPortEvent event) {
			try {
				// convert bytes to ASCII string discarding the trailing line-feed
				var bytes = event.getReceivedData();
				var message = new String(bytes, 0, bytes.length - 1, US_ASCII);
				log.info("received message from thermostat '{}': {}", thermostat.label, message);
				Stream.of(message.split(","))       // split message into fields  - e.g. [ "key1:value1", "key2:value2" ]
				      .map(part -> part.split(":")) // split field into key/value - e.g. [ "key1", "value1" ]
				      .forEach(keyValue -> {
				          switch (keyValue[0]) {
				              case "D":
				                  thermostat.desiredTemperature = Float.valueOf(keyValue[1]);
				                  break;
				              case "A":
				                  thermostat.ambientTemperature = Float.valueOf(keyValue[1]);
				                  break;
				              case "H":
				                  thermostat.heaterOn = "1".equals(keyValue[1]);
				                  break;
				              case "L":
				                  thermostat.remoteUpdateDisabled = "1".equals(keyValue[1]);
				                  break;
				          }
				      });
				thermostat.lastUpdate = Instant.now();
				repository.update(thermostat);
			} catch (Exception e) {
				log.error("problem handling message from thermostat '{}'", thermostat.label, e);
			}
		}

		@Override
		public int getListeningEvents() {
			return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
		}

		@Override
		public byte[] getMessageDelimiter() {
			return LF;
		}

		@Override
		public boolean delimiterIndicatesEndOfMessage() {
			return true;
		}
	};

	private volatile Writer writer;

	/**
	 * creates an instance for communicating with a thermostat
	 * 
	 * @param serialPort the serial port to connect on
	 * @param thermostat the thermostat to update
	 * @param repository the repository for updating the thermostat
	 */
	public ThermostatClient(SerialPort serialPort, Thermostat thermostat, ThermostatRepository repository) {
		this.serialPort = serialPort;
		this.thermostat = thermostat;
		this.repository = repository;
	}

	/**
	 * gets the last known state of the thermostat
	 * 
	 * @return the thermostat state
	 */
	public Thermostat getThermostat() {
		return thermostat;
	}

	/**
	 * connects to the thermostat, begins listening for updates, and request an initial update
	 * 
	 * @throws IOException if a failure occurs communicating with the thermostat on the serial port
	 * @throws IllegalStateException if already connected
	 */
	public synchronized void connect() throws IOException {
		if (isConnected()) {
			throw new IllegalStateException("already connected");
		}
		serialPort.setBaudRate(115200);
		serialPort.setNumDataBits(8);
		serialPort.setParity(SerialPort.NO_PARITY);
		serialPort.setNumStopBits(1);
		if (!serialPort.openPort()) {
			throw new IOException("failed to open serial port " + serialPort.getSystemPortName());
		}
		serialPort.addDataListener(listener);
		writer = new OutputStreamWriter(serialPort.getOutputStream(), US_ASCII);
		requestUpdate();
		log.info("connected to thermostat '{}'", thermostat.label);
	}

	/**
	 * stops listening for updates and disconnects from the thermostat
	 * 
	 * does nothing if not connected
	 */
	public void disconnect() {
		if (isConnected()) {
			serialPort.removeDataListener();
			serialPort.closePort();
			log.info("disconnected from thermostat '{}'", thermostat.label);
		}
	}

	/**
	 * determines whether this instance is connected to the thermostat
	 * 
	 * @return {@code true} if the serial port is open
	 */
	public boolean isConnected() {
		return serialPort.isOpen();
	}

	/**
	 * sends a message to the thermostat requesting an immediate update
	 * 
	 * @throws IOException if a failure occurs communicating with the thermostat on the serial port
	 * @throws IllegalStateException if not connected
	 */
	public void requestUpdate() throws IOException {
		writeMessage("U");
	}

	/**
	 * sends a message to the thermostat to set the desired temperature,
	 * then waits up to 5 seconds for a response, requesting an immediate update if necessary
	 * 
	 * @param desiredTemperature the desired temperature to set on the thermostat
	 * @return the updated state of the thermostat
	 * @throws IOException if a failure occurs communicating with the thermostat on the serial port
	 * @throws IllegalStateException if not connected or remote updates are disabled on the thermostat
	 * @throws TimeoutException if an update is not received within 5 seconds
	 * @throws InterruptedException if the thread is interrupted while waiting for an update
	 */
	public Thermostat setDesiredTemperature(float desiredTemperature)
			throws IOException, TimeoutException, InterruptedException {
		if (thermostat.remoteUpdateDisabled != null && thermostat.remoteUpdateDisabled.booleanValue()) {
			throw new IllegalStateException("remote updates are currently disabled by the thermostat");
		}
		var before = thermostat.lastUpdate;
		writeMessage(String.format("D:%f", desiredTemperature));
		for (int i = 0; i < 10; ++i) {
			Thread.sleep(500);
			if (!Objects.equals(before, thermostat.lastUpdate)) {
				return thermostat;
			}
			requestUpdate();
		}
		throw new TimeoutException("no update from thermostat within 5 seconds");
	}

	private synchronized void writeMessage(String message) throws IOException {
		if (!isConnected()) {
			throw new IllegalStateException("not connected");
		}
		writer.write(message);
		writer.write("\n");
		writer.flush();
	}
}
