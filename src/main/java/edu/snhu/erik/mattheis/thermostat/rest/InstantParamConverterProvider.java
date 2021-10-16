package edu.snhu.erik.mattheis.thermostat.rest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.Instant;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

/**
 * JAX-RS provider for producing converters used to convert ISO-8601 strings into Instants
 * 
 * @author <a href="mailto:erik.mattheis@snhu.edu">Erik Mattheis</a>
 */
@Provider
public class InstantParamConverterProvider implements ParamConverterProvider {
	/**
	 * if the raw type is equal to {@link Instant Instant.class} - returns
	 * a {@link ParamConverter}{@link Instant &lt;Instant&gt;} which converts
	 * from and to string values using {@link Instant#parse(CharSequence)}
	 * and {@link Instant#toString()}, respectively - otherwise returns {@code null}
	 */
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
