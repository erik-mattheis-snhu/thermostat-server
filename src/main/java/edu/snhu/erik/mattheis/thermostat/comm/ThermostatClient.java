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

public class ThermostatClient {
	
	private static final byte[] LF = { 0x0A };

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ThermostatRepository repository;
	private final Thermostat thermostat;
	private final SerialPort serialPort;
	private final SerialPortMessageListener listener = new SerialPortMessageListener() {
		public void serialEvent(SerialPortEvent event) {
			try {
				var data = event.getReceivedData();
				var message = new String(data, 0, data.length - 1, US_ASCII)	;
				log.info("received message from thermostat '{}': {}", thermostat.label, message);
				Stream.of(message.split(","))
						.map(part -> part.split(":"))
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

	public ThermostatClient(SerialPort serialPort, Thermostat thermostat, ThermostatRepository repository) {
		this.serialPort = serialPort;
		this.thermostat = thermostat;
		this.repository = repository;
	}

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

	public void disconnect() {
		if (serialPort.isOpen()) {
			serialPort.removeDataListener();
			serialPort.closePort();
			log.info("disconnected from thermostat '{}'", thermostat.label);
		}
	}

	public boolean isConnected() {
		return serialPort.isOpen();
	}

	public void requestUpdate() throws IOException {
		writeMessage("U");
	}

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

	public Thermostat getThermostat() {
		return thermostat;
	}
}
