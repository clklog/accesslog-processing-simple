package com.zcunsoft.accesslog.processing.cfg;

import nl.basjes.parse.useragent.AbstractUserAgentAnalyzer;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAgentAnalyzerConfig {
	@Bean
	public AbstractUserAgentAnalyzer userAgentAnalyzer() {
		UserAgentAnalyzer userAgentAnalyzer = UserAgentAnalyzer.newBuilder().withField(UserAgent.AGENT_NAME)
				.withField(UserAgent.AGENT_NAME_VERSION)
				.withField(UserAgent.DEVICE_NAME)
				.withField(UserAgent.DEVICE_BRAND).hideMatcherLoadStats().withCache(10000)
				.build();

		return userAgentAnalyzer;
	}
}
