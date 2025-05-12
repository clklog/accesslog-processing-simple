CREATE DATABASE accesslogdb ENGINE = Atomic;

CREATE TABLE accesslogdb.gp_nginx_access
(
    `country` String,
    `upstream_uri` String,
    `upstream_addr` String,
    `uri` String,
    `request_method` String,
    `http_host` String,
    `http_user_agent` String,
    `stat_hour` String,
    `manufacturer` String,
    `remote_user` String,
    `upstream_status` String,
    `request_time` Float32,
    `province` String,
    `browser` String,
    `model` String,
    `browser_version` String,
    `brand` String,
    `remote_addr` String,
    `stat_date` Date,
    `stat_min` String,
    `time_local` DateTime64(3),
    `http_version` String,
    `city` String,
    `body_sent_bytes` String,
    `http_referrer` String,
    `server_name` String,
    `upstream_response_time` String,
    `status` String,
    `application_code` String,
    `create_time` DateTime64(3) DEFAULT now(),
    `raw_uri` String
)
    ENGINE = MergeTree
PARTITION BY stat_date
ORDER BY (application_code, http_host, status, request_method, remote_addr, country, province, city, uri, browser, time_local)
SETTINGS index_granularity = 8192;


CREATE TABLE accesslogdb.tbl_app
(
    `app_code` String,
    `app_name` String,
    `owner` String,
    `create_time` DateTime64(3)
)
    ENGINE = MergeTree
PARTITION BY app_code
ORDER BY create_time
SETTINGS index_granularity = 8192;
