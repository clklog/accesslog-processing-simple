package com.zcunsoft.accesslog.processing.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Data
public class AccessLog {
    private String country;
    private String upstreamUri;
    private String upstreamAddr;
    private String uri;
    private String requestMethod;
    private String httpHost;
    private String httpUserAgent;
    private String statHour;
    private String manufacturer;
    private String remoteUser;
    private String upstreamStatus;
    private Float requestTime;
    private String province;
    private String browser;
    private String model;
    private String browserVersion;
    private String brand;
    private String remoteAddr;
    private Date statDate;
    private String statMin;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private Timestamp time;
    private String httpVersion;
    private String city;
    private String bodySentBytes;
    private String httpReferrer;
    private String serverName;
    private String upstreamResponseTime;
    private String status;
    private String applicationCode;
}
