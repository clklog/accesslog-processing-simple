package com.zcunsoft.accesslog.processing.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ip2location.IP2Location;
import com.ip2location.IPResult;
import com.zcunsoft.accesslog.processing.cfg.DealServiceSetting;
import com.zcunsoft.accesslog.processing.handlers.ConstsDataHolder;
import com.zcunsoft.accesslog.processing.models.AccessLog;
import com.zcunsoft.accesslog.processing.models.Region;
import com.zcunsoft.accesslog.processing.utils.ObjectMapperUtil;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.Match;
import nl.basjes.parse.useragent.AbstractUserAgentAnalyzer;
import nl.basjes.parse.useragent.AgentField;
import nl.basjes.parse.useragent.UserAgent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

@Service
public class ReceiveServiceImpl implements IReceiveService {

    private final InetAddressValidator validator = InetAddressValidator.getInstance();

    private final ConstsDataHolder constsDataHolder;

    private final Logger logger = LogManager.getLogger(this.getClass());

    private final ObjectMapperUtil objectMapper;

    private final StringRedisTemplate queueRedisTemplate;

    private final AbstractUserAgentAnalyzer userAgentAnalyzer;

    private final IP2Location locIpV4 = new IP2Location();

    private final IP2Location locIpV6 = new IP2Location();

    private final JdbcTemplate clickHouseJdbcTemplate;

    private final DealServiceSetting serverSetting;

    private static final ThreadLocal<DateFormat> formatter = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        }
    };

    private static final ThreadLocal<DateFormat> HH = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("HH");
        }
    };

    private static final ThreadLocal<DateFormat> HHmm = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("HH:mm");
        }
    };

    private static final ThreadLocal<DecimalFormat> decimalFormat =
            new ThreadLocal<DecimalFormat>() {
                @Override
                protected DecimalFormat initialValue() {
                    return new DecimalFormat("0.##");
                }
            };


    private final Grok accesslogGrok;

    private final TypeReference<AccessLog> accessLogTypeReference = new TypeReference<AccessLog>() {
    };


    public ReceiveServiceImpl(ConstsDataHolder constsDataHolder, ObjectMapperUtil objectMapper, StringRedisTemplate queueRedisTemplate, AbstractUserAgentAnalyzer userAgentAnalyzer, JdbcTemplate clickHouseJdbcTemplate, DealServiceSetting serverSetting,
                              Grok accesslogGrok) {
        this.objectMapper = objectMapper;
        this.queueRedisTemplate = queueRedisTemplate;
        this.userAgentAnalyzer = userAgentAnalyzer;
        this.constsDataHolder = constsDataHolder;
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
        this.serverSetting = serverSetting;
        this.accesslogGrok = accesslogGrok;
        String binIpV4file = getResourcePath() + File.separator + "iplib" + File.separator + "IP2LOCATION-LITE-DB3.BIN";

        try {
            locIpV4.Open(binIpV4file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String binIpV6file = getResourcePath() + File.separator + "iplib" + File.separator + "IP2LOCATION-LITE-DB3.IPV6.BIN";

        try {
            locIpV6.Open(binIpV6file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Region analysisRegionFromIp(String clientIp) {
        Region region = new Region();
        region.setClientIp(clientIp);
        String regionInfo = (String) queueRedisTemplate.opsForHash().get("ClientIpRegionHash", clientIp);
        if (regionInfo == null) {
            IPResult rec = null;
            if (validator.isValidInet4Address(clientIp)) {
                rec = analysisIp(true, clientIp);
            } else if (validator.isValidInet6Address(clientIp)) {
                rec = analysisIp(false, clientIp);
            }

            if (rec != null && rec.getStatus().equalsIgnoreCase("OK")) {
                String country = rec.getCountryShort().toLowerCase(Locale.ROOT);
                String province = rec.getRegion().toLowerCase(Locale.ROOT);
                String city = rec.getCity().toLowerCase(Locale.ROOT);
                if ("-".equalsIgnoreCase(country)) {
                    country = "";
                }
                if ("-".equalsIgnoreCase(province)) {
                    province = "";
                }
                if ("-".equalsIgnoreCase(city)) {
                    city = "";
                }
                if (StringUtils.isNotBlank(country)) {
                    if (country.equalsIgnoreCase("TW")) {
                        country = "cn";
                        province = "taiwan";
                    }
                    if (country.equalsIgnoreCase("hk")) {
                        country = "cn";
                        province = "hongkong";
                        city = "hongkong";
                    }
                    if (country.equalsIgnoreCase("mo")) {
                        country = "cn";
                        province = "macau";
                        city = "macau";
                    }
                    if (constsDataHolder.getHtForCountry().containsKey(country)) {
                        country = constsDataHolder.getHtForCountry().get(country);
                    }
                }
                if (StringUtils.isNotBlank(province)) {
                    if (constsDataHolder.getHtForProvince().containsKey(province)) {
                        province = constsDataHolder.getHtForProvince().get(province);
                    }
                }
                if (StringUtils.isNotBlank(city)) {
                    if (constsDataHolder.getHtForCity().containsKey(city)) {
                        city = constsDataHolder.getHtForCity().get(city);
                    }
                }

                region.setCountry(country);
                region.setProvince(province);
                region.setCity(city);

                String sbRegion = region.getClientIp() + "," + region.getCountry() + "," + region.getProvince() + "," + region.getCity();
                queueRedisTemplate.opsForHash().put("ClientIpRegionHash", region.getClientIp(), sbRegion);
            }
        } else {
            String[] regionArr = regionInfo.split(",", -1);
            if (regionArr.length == 4) {
                region.setCountry(regionArr[1]);
                region.setProvince(regionArr[2]);
                region.setCity(regionArr[3]);
            }
        }
        return region;
    }

    private IPResult analysisIp(boolean isIpV4, String clientIp) {
        IPResult rec = null;
        try {
            if (isIpV4) {
                rec = locIpV4.IPQuery(clientIp);
            } else {
                rec = locIpV6.IPQuery(clientIp);
            }
            //    loc.Close();
        } catch (Exception e) {
            logger.error("query ip error ", e);
        }
        return rec;
    }

    @Override
    public AccessLog analysisData(String message) {
        AccessLog accessLog = null;

        try {
            accessLog = extractToAccessLog(message, userAgentAnalyzer);
            if (accessLog != null && StringUtils.isNotBlank(accessLog.getRemoteAddr())) {
                Region region = analysisRegionFromIp(accessLog.getRemoteAddr());

                accessLog.setCountry(region.getCountry());
                accessLog.setProvince(region.getProvince());
                accessLog.setCity(region.getCity());
            }
        } catch (Exception ex) {
            logger.error("analysisData err", ex);
        }

        return accessLog;
    }

    @Override
    public void saveToClickHouse(List<String> logList) {
        List<AccessLog> allList = new ArrayList<>();
        for (String log : logList) {
            AccessLog accessLog = analysisData(log);
            if (accessLog != null) {
                allList.add(accessLog);
            }
        }
        doSaveToClickHouse(allList);
    }

    private void doSaveToClickHouse(List<AccessLog> accessLogList) {

        String sql = "insert into " + serverSetting.getNginxAccessTable() + " (country,upstream_uri,upstream_addr,uri,request_method,http_host,http_user_agent," +
                "stat_hour,manufacturer,remote_user,upstream_status,request_time,province,browser,model,browser_version," +
                "brand,remote_addr,stat_date,stat_min,time_local,http_version,city,body_sent_bytes,http_referrer," +
                "server_name,upstream_response_time,status,application_code,create_time,raw_uri) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        clickHouseJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement pst, int i) throws SQLException {
                AccessLog accessLog = accessLogList.get(i);
                pst.setString(1, accessLog.getCountry());
                pst.setString(2, StringUtils.defaultString(accessLog.getUpstreamUri()));
                pst.setString(3, StringUtils.defaultString(accessLog.getUpstreamAddr()));
                pst.setString(4, accessLog.getUri());
                pst.setString(5, accessLog.getRequestMethod());
                pst.setString(6, StringUtils.defaultString(accessLog.getHttpHost()));
                pst.setString(7, accessLog.getHttpUserAgent());
                pst.setString(8, accessLog.getStatHour());
                pst.setString(9, StringUtils.defaultString(accessLog.getManufacturer()));
                pst.setString(10, StringUtils.defaultString(accessLog.getRemoteUser()));
                pst.setString(11, StringUtils.defaultString(accessLog.getUpstreamStatus()));
                pst.setFloat(12, accessLog.getRequestTime());
                pst.setString(13, accessLog.getProvince());
                pst.setString(14, accessLog.getBrowser());
                pst.setString(15, accessLog.getModel());
                pst.setString(16, accessLog.getBrowserVersion());
                pst.setString(17, accessLog.getBrand());
                pst.setString(18, accessLog.getRemoteAddr());
                pst.setDate(19, accessLog.getStatDate());
                pst.setString(20, accessLog.getStatMin());
                pst.setTimestamp(21, accessLog.getTime());
                pst.setString(22, StringUtils.defaultString(accessLog.getHttpVersion()));
                pst.setString(23, accessLog.getCity());
                pst.setString(24, StringUtils.defaultString(accessLog.getBodySentBytes()));
                pst.setString(25, StringUtils.defaultString(accessLog.getHttpReferrer()));
                pst.setString(26, accessLog.getServerName());
                pst.setString(27, StringUtils.defaultString(accessLog.getUpstreamResponseTime()));
                pst.setString(28, accessLog.getStatus());
                pst.setString(29, accessLog.getApplicationCode());
                pst.setTimestamp(30, new Timestamp(System.currentTimeMillis()));
                pst.setString(31, accessLog.getRawUri());
            }

            @Override
            public int getBatchSize() {
                return accessLogList.size();
            }
        });
    }

    @Override
    public void loadCity() {
        try {
            List<String> lineCityList = FileUtils.readLines(new File(
                    getResourcePath() + File.separator + "iplib" + File.separator
                            + "chinacity.txt"), Charset.forName("GB2312"));

            ConcurrentMap<String, String> htForCity = constsDataHolder.getHtForCity();
            for (String line : lineCityList) {

                String[] pair = line.split(",");
                if (pair.length >= 2) {
                    htForCity.put(pair[0].toLowerCase(Locale.ROOT), pair[1]);
                }
            }
        } catch (Exception ex) {
            logger.error("load City err", ex);
        }
    }

    @Override
    public void loadProvince() {
        try {
            List<String> lineProvinceList = FileUtils.readLines(new File(getResourcePath() + File.separator + "iplib" + File.separator
                    + "chinaprovince.txt"), Charset.forName("GB2312"));

            ConcurrentMap<String, String> htForProvince = constsDataHolder.getHtForProvince();
            for (String line : lineProvinceList) {

                String[] pair = line.split(",");
                if (pair.length >= 2) {
                    htForProvince.put(pair[0].toLowerCase(Locale.ROOT), pair[1]);
                }
            }
        } catch (Exception ex) {
            logger.error("load Province err", ex);
        }
    }

    @Override
    public void loadCountry() {
        try {
            List<String> countryList = FileUtils.readLines(new File(getResourcePath() + File.separator + "iplib" + File.separator
                    + "country.txt"), Charset.forName("GB2312"));

            ConcurrentMap<String, String> htForCountry = constsDataHolder.getHtForCountry();
            for (String line : countryList) {

                String[] pair = line.split(",");
                if (pair.length >= 2) {
                    htForCountry.put(pair[0].toLowerCase(Locale.ROOT), pair[1]);
                }
            }

        } catch (Exception ex) {
            logger.error("load Country err", ex);
        }
    }


    public AccessLog extractToAccessLog(String log, Grok grok, AbstractUserAgentAnalyzer userAgentAnalyzer) throws ParseException {
        AccessLog accessLog = new AccessLog();

        Match gm = grok.match(log);
        Map<String, Object> capture = gm.capture();

        if (capture.containsKey("upstream-uri")) {
            accessLog.setUpstreamUri(capture.get("upstream-uri").toString());
        }
        if (capture.containsKey("upstream_addr")) {
            accessLog.setUpstreamAddr(capture.get("upstream_addr").toString());
        }
        if (capture.containsKey("uri")) {
            String uri = capture.get("uri").toString();
            int index = uri.indexOf("?");
            if (index != -1) {
                uri = uri.substring(0, index);
            }
            accessLog.setUri(uri);
        }
        if (capture.containsKey("request_method")) {
            accessLog.setRequestMethod(capture.get("request_method").toString());
        }
        if (capture.containsKey("host")) {
            accessLog.setHttpHost(capture.get("host").toString());
        }
        if (capture.containsKey("http_user_agent")) {
            accessLog.setHttpUserAgent(capture.get("http_user_agent").toString());
        }
        if (capture.containsKey("remote_user")) {
            accessLog.setRemoteUser(capture.get("remote_user").toString());
        }
        if (capture.containsKey("upstream_status")) {
            accessLog.setUpstreamStatus(capture.get("upstream_status").toString());
        }
        if (capture.containsKey("request_time")) {
            accessLog.setRequestTime(Float.parseFloat(capture.get("request_time").toString()) * 1000);
        }
        if (capture.containsKey("http_version")) {
            accessLog.setHttpVersion(capture.get("http_version").toString());
        }
        if (capture.containsKey("body_sent_bytes")) {
            accessLog.setBodySentBytes(capture.get("body_sent_bytes").toString());
        }
        if (capture.containsKey("http_referrer")) {
            accessLog.setHttpReferrer(capture.get("http_referrer").toString());
        }
        if (capture.containsKey("upstream_response_time")) {
            String upStreamRespTime = capture.get("upstream_response_time").toString();
            if (!upStreamRespTime.equalsIgnoreCase("-")) {
                try {
                    float fRespTime = Float.parseFloat(upStreamRespTime) * 1000;
                    upStreamRespTime = decimalFormat.get().format(fRespTime);
                } catch (Exception ex) {
                    logger.error("parse upstream_response_time error " + log);
                }
            }
            accessLog.setUpstreamResponseTime(upStreamRespTime);
        }
        if (capture.containsKey("status")) {
            accessLog.setStatus(capture.get("status").toString());
        }
        if (capture.containsKey("remote_addr")) {
            accessLog.setRemoteAddr(capture.get("remote_addr").toString());
        }
        if (capture.containsKey("time")) {
            Date date = formatter.get().parse(capture.get("time").toString());
            accessLog.setStatDate(new java.sql.Date(date.getTime()));
            accessLog.setStatHour(HH.get().format(date));
            accessLog.setStatMin(HHmm.get().format(date));
            accessLog.setTime(new Timestamp(date.getTime()));
        }
        accessLog.setServerName(serverSetting.getServerName());
        accessLog.setApplicationCode(serverSetting.getApplicationCode());

        if (StringUtils.isNotBlank(accessLog.getHttpUserAgent())) {
            UserAgent userAgent = userAgentAnalyzer.parse(accessLog.getHttpUserAgent());

            String browser = "";
            AgentField browserField = userAgent.get(UserAgent.AGENT_NAME);
            if (!browserField.isDefaultValue()) {
                browser = browserField.getValue();
            }
            accessLog.setBrowser(browser);

            String browserVersion = "";
            AgentField browserVersionField = userAgent.get(UserAgent.AGENT_NAME_VERSION);
            if (!browserVersionField.isDefaultValue()) {
                browserVersion = browserVersionField.getValue();
            }
            accessLog.setBrowserVersion(browserVersion);

            String model = "";
            AgentField deviceName = userAgent.get(UserAgent.DEVICE_NAME);
            if (!deviceName.isDefaultValue()) {
                model = deviceName.getValue();
            }
            accessLog.setModel(model);

            String brand = "";
            AgentField deviceBrand = userAgent.get(UserAgent.DEVICE_BRAND);
            if (!deviceBrand.isDefaultValue()) {
                brand = deviceBrand.getValue();
            }
            accessLog.setBrand(brand);
            accessLog.setManufacturer(brand);
        }

        return accessLog;
    }

    public AccessLog extractToAccessLog(String log, AbstractUserAgentAnalyzer userAgentAnalyzer) throws ParseException {
        AccessLog accessLog = null;
        try {
            accessLog = objectMapper.readValue(log, accessLogTypeReference);

            accessLog.setRawUri(accessLog.getUri());
            String uri = accessLog.getUri();
            int index = uri.indexOf("?");
            if (index != -1) {
                uri = uri.substring(0, index);
            }
            accessLog.setUri(uri);

            if (StringUtils.isNotBlank(accessLog.getHttpUserAgent())) {
                UserAgent userAgent = userAgentAnalyzer.parse(accessLog.getHttpUserAgent());

                String browser = "";
                AgentField browserField = userAgent.get(UserAgent.AGENT_NAME);
                if (!browserField.isDefaultValue()) {
                    browser = browserField.getValue();
                }
                accessLog.setBrowser(browser);

                String browserVersion = "";
                AgentField browserVersionField = userAgent.get(UserAgent.AGENT_NAME_VERSION);
                if (!browserVersionField.isDefaultValue()) {
                    browserVersion = browserVersionField.getValue();
                }
                accessLog.setBrowserVersion(browserVersion);

                String model = "";
                AgentField deviceName = userAgent.get(UserAgent.DEVICE_NAME);
                if (!deviceName.isDefaultValue()) {
                    model = deviceName.getValue();
                }
                accessLog.setModel(model);

                String brand = "";
                AgentField deviceBrand = userAgent.get(UserAgent.DEVICE_BRAND);
                if (!deviceBrand.isDefaultValue()) {
                    brand = deviceBrand.getValue();
                }
                accessLog.setBrand(brand);
                accessLog.setManufacturer(brand);
            }
        } catch (JsonProcessingException e) {
            logger.error("extractToAccessLog err" + log, e);
        }
        return accessLog;
    }

    private String getResourcePath() {
        if (StringUtils.isBlank(serverSetting.getResourcePath())) {
            return System.getProperty("user.dir");
        } else {
            return serverSetting.getResourcePath();
        }
    }
}
