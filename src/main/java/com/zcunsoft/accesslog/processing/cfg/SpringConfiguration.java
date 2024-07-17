package com.zcunsoft.accesslog.processing.cfg;

import com.zcunsoft.accesslog.processing.handlers.ConstsDataHolder;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
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

    @Bean
    public Grok accesslogGrok() {
        GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();

        Grok grok = grokCompiler.compile(serverSetting.getAccesslogGrok());

        return grok;
    }
}
