package com.zcunsoft.accesslog.processing;

import com.zcunsoft.accesslog.processing.cfg.DealServiceSetting;
import com.zcunsoft.accesslog.processing.models.AccessLog;
import com.zcunsoft.accesslog.processing.services.IReceiveService;
import com.zcunsoft.accesslog.processing.utils.ObjectMapperUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest
public class AppTest {
    @Autowired
    private DealServiceSetting serverSetting;

    @Autowired
    private IReceiveService logService;

    @Autowired
    private ObjectMapperUtil objectMapper;

    private Object getFieldValueByName(String fieldName, Object o) {
        try {
            String firstLetter = fieldName.substring(0, 1).toUpperCase();
            String getter = "get" + firstLetter + fieldName.substring(1);
            Method method = o.getClass().getMethod(getter, new Class[]{});
            Object value = method.invoke(o, new Object[]{});
            if (value != null) {
                value = value.toString();
            }
            return value;
        } catch (Exception e) {

            return null;
        }
    }

    private List<Object> getFieldsInfo(Object o) {
        Field[] fields = o.getClass().getDeclaredFields();
        List<Object> list = new ArrayList();
        for (int i = 0; i < fields.length; i++) {
            list.add(getFieldValueByName(fields[i].getName(), o));
        }
        return list;
    }

    @TestFactory
    Collection<DynamicTest> dynamicTestExtractToAccesslog() throws Exception {

        String message = "{\"application_code\":\"accesslog\",\"http_referrer\":\"https://app.accesslog.com/\",\"http_user_agent\":\"Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Kuang/2.2.0\",\"port\":\"80\",\"remote_addr\":\"183.195.96.16\",\"remote_user\":\"-\",\"request_method\":\"POST\",\"request_time\":4.0,\"server_name\":\"accesslog_server\",\"stat_date\":\"2024-01-18\",\"stat_hour\":\"09\",\"stat_min\":\"09:10\",\"status\":\"200\",\"substatus\":\"0\",\"time\":\"2024-01-18 09:10:49\",\"time_local\":\"2024-01-18T01:10:49+08:00\",\"uri\":\"/pub/goods/comments/unreadcount\",\"uri_query\":\"-\",\"uri_stem\":\"/pub/goods/comments/unreadcount\",\"win32_status\":\"0\",\"upstream_addr\":\"10.245.0.182:8181\",\"http_host\":\"accesslog.com\",\"upstream_status\":\"200\",\"http_version\":\"2.0\",\"body_sent_bytes\":\"64\",\"upstream_response_time\":\"14\"}";
        AccessLog actualLog = logService.analysisData(message);
        Object[] actualArr = getFieldsInfo(actualLog).toArray();

        AccessLog expectedLog = new AccessLog();
        expectedLog.setUpstreamAddr("10.245.0.182:8181");
        expectedLog.setUri("/pub/goods/comments/unreadcount");
        expectedLog.setRawUri("/pub/goods/comments/unreadcount");
        expectedLog.setRequestMethod("POST");
        expectedLog.setHttpHost("accesslog.com");
        expectedLog.setHttpUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Kuang/2.2.0");
        expectedLog.setRemoteUser("-");
        expectedLog.setUpstreamStatus("200");
        expectedLog.setRequestTime(4f);
        expectedLog.setHttpVersion("2.0");
        expectedLog.setBodySentBytes("64");
        expectedLog.setHttpReferrer("https://app.accesslog.com/");
        expectedLog.setUpstreamResponseTime("14");
        expectedLog.setStatus("200");
        expectedLog.setRemoteAddr("183.195.96.16");
        expectedLog.setStatDate("2024-01-18");
        expectedLog.setStatHour("09");
        expectedLog.setStatMin("09:10");
        expectedLog.setTime(Timestamp.valueOf("2024-01-18 09:10:49"));
        expectedLog.setServerName("accesslog_server");
        expectedLog.setApplicationCode("accesslog");
        expectedLog.setBrowser("Kuang");
        expectedLog.setBrowserVersion("Kuang 2.2.0");
        expectedLog.setModel("Apple iPhone");
        expectedLog.setBrand("Apple");
        expectedLog.setManufacturer("Apple");
        expectedLog.setCountry("中国");
        expectedLog.setProvince("上海");
        expectedLog.setCity("上海");
        Object[] expectedArr = getFieldsInfo(expectedLog).toArray();

        List<DynamicTest> dynamicTestList = new ArrayList<>();
        dynamicTestList.add(dynamicTest("test1 extractToLogBean dynamic test", () -> Assertions.assertArrayEquals(expectedArr, actualArr, "ok")));

        return dynamicTestList;
    }
}
