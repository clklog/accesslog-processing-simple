package com.zcunsoft.accesslog.processing.services;

import com.zcunsoft.accesslog.processing.models.AccessLog;
import com.zcunsoft.accesslog.processing.models.Region;

import java.util.List;

public interface IReceiveService {
    Region analysisRegionFromIp(String clientIp);

    AccessLog analysisData(String message);

    void saveToClickHouse(List<String> logList);

    void loadCity();

}
