package com.zcunsoft.accesslog.processing.cfg;

import com.zcunsoft.accesslog.processing.handlers.ConstsDataHolder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties({DealServiceSetting.class})
public class SpringConfiguration {
    @Resource
    private DealServiceSetting serverSetting;

    @Bean
    public ConstsDataHolder constsDataHolder() {
        return new ConstsDataHolder();
    }

}
