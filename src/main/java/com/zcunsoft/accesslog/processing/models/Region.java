package com.zcunsoft.accesslog.processing.models;

import lombok.Data;

@Data
public class Region {
    private String clientIp = "";

    private String country = "";

    private String province = "";

    private String city = "";
}
