package com.zcunsoft.accesslog.processing.handlers;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
public class ConstsDataHolder {

	private final ConcurrentMap<String, String> htForCountry = new ConcurrentHashMap<String, String>();

	private final ConcurrentMap<String, String> htForProvince = new ConcurrentHashMap<String, String>();

	private final ConcurrentMap<String, String> htForCity = new ConcurrentHashMap<String, String>();

}
