package com.zcunsoft.accesslog.processing.daemon;

import com.zcunsoft.accesslog.processing.cfg.DealServiceSetting;
import com.zcunsoft.accesslog.processing.services.IReceiveService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;

@Component
public class LogDealProcessBoss {
    private final Logger logger = LogManager.getLogger(this.getClass());

    @Resource
    private DealServiceSetting serverSetting;

    @Resource
    private IReceiveService logService;

    List<Thread> threadList = null;

    boolean running = false;

    @PostConstruct
    public void start() throws Exception {
        running = true;
        threadList = new ArrayList<Thread>();
        if (serverSetting.getThreadCount() > 0) {
            for (int i = 0; i < serverSetting.getThreadCount(); i++) {
                int j = i;
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        work(j);
                    }
                }, "LogDealProcess-" + String.valueOf(i));

                thread.start();
                threadList.add(thread);
            }
        }
    }


    void work(int threadId) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(getConsumerConfig());
        consumer.subscribe(Arrays.asList(serverSetting.getTopicName()));
        try {
            while (running) {
                try {
                    List<String> logList = new ArrayList<String>();
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(2000));

                    for (final ConsumerRecord<String, String> record : records) {
                        logList.add(record.value());
                    }

                    if (!logList.isEmpty()) {
                        logService.saveToClickHouse(logList);
                    }
                } catch (Exception e) {
                    logger.error("deal error", e);
                }
            }
        } finally {
            consumer.close();
        }
    }

    private Map<String, Object> getConsumerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverSetting.getKafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, serverSetting.getKafkaConsumerGroupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    @PreDestroy
    public void stop() {
        running = false;
        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (logger.isInfoEnabled()) {
                logger.info(thread.getName() + " stopping...");
            }
        }
    }
}