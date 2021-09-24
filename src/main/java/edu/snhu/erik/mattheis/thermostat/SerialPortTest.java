package edu.snhu.erik.mattheis.thermostat;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

public class SerialPortTest {
	
	private static final byte[] LF = { 0x0A };
	
	private static SerialPortMessageListener listener(Consumer<String> listener) {
		return new SerialPortMessageListener() {
			public void serialEvent(SerialPortEvent event) {
				byte[] data = event.getReceivedData();
				String string = new String(data, 0, data.length - 1, US_ASCII);
				listener.accept(string);
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
	}
	
	public static void main(String... args) throws IOException {
		SerialPort port = SerialPort.getCommPort("cu.usbmodemE00810101");

		port.setBaudRate(115200);
		port.setNumDataBits(8);
		port.setParity(SerialPort.NO_PARITY);
		port.setNumStopBits(1);
		port.openPort();
		port.addDataListener(listener(System.out::println));
		
		try (Writer out = new OutputStreamWriter(port.getOutputStream(), US_ASCII)) {
			while (port.isOpen()) {
				out.write("U\n");
				out.flush();
				TimeUnit.SECONDS.sleep(1);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		port.removeDataListener();
		port.closePort();
	}
}
