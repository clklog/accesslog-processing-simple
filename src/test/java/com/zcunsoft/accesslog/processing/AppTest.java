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
import java.util.HashMap;
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

        String log = "183.195.96.16 - [183.195.96.16] - - [18/Jan/2024:09:10:49 +0800] \"POST /pub/goods/comments/unreadcount?time=123 HTTP/2.0\" 200 64 \"https://app.accesslog.com/\" \"Mozilla/5.0 (iPhone; CPU iPhone OS 17_2_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Kuang/2.2.0\" 47 0.004 [accesslog-backend-80] 10.245.0.182:8181 75 0.014 200 a9ade1763ef2ddf3e54465a6b2a5b179 accesslog.com []";

        HashMap<String, String> hashMapLog = new HashMap<>();
        hashMapLog.put("stream", "stdout");
        hashMapLog.put("log", log);

        String message = objectMapper.writeValueAsString(hashMapLog);

        AccessLog actualLog = logService.analysisData(message);
        Object[] actualArr = getFieldsInfo(actualLog).toArray();

        AccessLog expectedLog = new AccessLog();
        expectedLog.setUpstreamAddr("10.245.0.182:8181");
        expectedLog.setUri("/pub/goods/comments/unreadcount");
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
        expectedLog.setStatDate(java.sql.Date.valueOf("2024-01-18"));
        expectedLog.setStatHour("09");
        expectedLog.setStatMin("09:10");
        expectedLog.setTime(Timestamp.valueOf("2024-01-18 09:10:49"));
        expectedLog.setServerName(serverSetting.getServerName());
        expectedLog.setApplicationCode(serverSetting.getApplicationCode());
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
