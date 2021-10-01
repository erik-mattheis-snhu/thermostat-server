package edu.snhu.erik.mattheis.thermostat.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

@Provider
public class InstantParamConverterProvider implements ParamConverterProvider {
	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
		if (rawType.equals(Instant.class)) {
			return (ParamConverter<T>) new ParamConverter<Instant>() {
				@Override
				public Instant fromString(String value) {
					return value == null ? null : Instant.parse(value);
				}

				@Override
				public String toString(Instant value) {
					return value == null ? null : value.toString();
				}
			};
		}
		return null;
	}
}
