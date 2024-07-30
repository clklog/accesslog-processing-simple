package com.zcunsoft.accesslog.processing.cfg;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("accesslog-processing")
public class DealServiceSetting {

    private String kafkaBootstrapServers;

    private String kafkaConsumerGroupId;

    private int threadCount = 6;

    private String nginxAccessTable = "";

    private String topicName = "accesslog";

    private String resourcePath = "";


    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public String getKafkaConsumerGroupId() {
        return kafkaConsumerGroupId;
    }

    public void setKafkaConsumerGroupId(String kafkaConsumerGroupId) {
        this.kafkaConsumerGroupId = kafkaConsumerGroupId;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getNginxAccessTable() {
        return nginxAccessTable;
    }

    public void setNginxAccessTable(String nginxAccessTable) {
        this.nginxAccessTable = nginxAccessTable;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

}
