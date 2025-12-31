CREATE TABLE IF NOT EXISTS gp_nginx_access
(
    country LowCardinality(String),
    upstream_uri String,
    upstream_addr LowCardinality(String),
    uri String,
    request_method LowCardinality(String),
    http_host LowCardinality(String),
    http_user_agent String,
    stat_hour String,
    manufacturer String,
    remote_user String,
    upstream_status LowCardinality(String),
    request_time Float32,
    province String,
    browser String,
    model String,
    browser_version String,
    brand String,
    remote_addr String,
    stat_date Date,
    stat_min String,
    time_local DateTime64(3),
    http_version LowCardinality(String),
    city String,
    body_sent_bytes UInt64,
    http_referrer String,
    server_name LowCardinality(String),
    upstream_response_time String,
    status UInt16,
    application_code LowCardinality(String),
    create_time DateTime64(3) DEFAULT now64(3),
    raw_uri String
)
ENGINE = MergeTree
PARTITION BY stat_date
ORDER BY (application_code, http_host, time_local, uri);

CREATE TABLE IF NOT EXISTS tbl_app
(
    app_code String,
    app_name String,
    owner String,
    create_time DateTime64(3)
)
ENGINE = ReplacingMergeTree
ORDER BY app_code;
