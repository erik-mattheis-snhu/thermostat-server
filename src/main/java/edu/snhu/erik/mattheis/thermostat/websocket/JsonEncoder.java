package edu.snhu.erik.mattheis.thermostat.websocket;

import javax.enterprise.inject.spi.CDI;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * a websocket text encoder that looks up an {@link ObjectMapper}
 * instance using CDI and uses it to encode objects as JSON
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
public class JsonEncoder implements Encoder.Text<Object> {

	private ObjectMapper jackson;

	/**
	 * looks up an {@link ObjectMapper} instance programmatically using
	 * CDI since injection is not supported in {@link Encoder} instances
	 */
	@Override
	public void init(EndpointConfig config) {
		jackson = CDI.current().select(ObjectMapper.class).get();
	}

	/**
	 * nulls out the {@link ObjectMapper} reference
	 */
	@Override
	public void destroy() {
		jackson = null;
	}

	/**
	 * encodes the supplied object to JSON using
	 * {@link ObjectMapper#writeValueAsString(Object)}
	 */
	@Override
	public String encode(Object object) throws EncodeException {
		try {
			return jackson.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new EncodeException(object, "problem ecoding object as JSON", e);
		}
	}
}
