package com.zcunsoft.accesslog.processing.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public class ObjectMapperUtil extends ObjectMapper {
	private static final long serialVersionUID = 1L;

	public ObjectMapperUtil() {
		super();
		setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
		configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
}
