package edu.snhu.erik.mattheis.thermostat.websocket;

import javax.enterprise.inject.spi.CDI;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonEncoder implements Encoder.Text<Object> {

	private ObjectMapper jackson;

	@Override
	public void init(EndpointConfig config) {
		// injection is not support for eccoders, so lookup manually
		jackson = CDI.current().select(ObjectMapper.class).get();
	}

	@Override
	public void destroy() {
		jackson = null;
	}

	@Override
	public String encode(Object object) throws EncodeException {
		try {
			return jackson.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new EncodeException(object, "problem ecoding object as JSON", e);
		}
	}
}
