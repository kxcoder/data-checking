package com.example.datachecking.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Value("${data-check.executor.core-size:8}")
    private int coreSize;
    
    @Value("${data-check.executor.max-size:16}")
    private int maxSize;
    
    @Value("${data-check.executor.queue-capacity:1000}")
    private int queueCapacity;
    
    @Value("${data-check.fail-executor.core-size:2}")
    private int failCoreSize;
    
    @Value("${data-check.fail-executor.max-size:4}")
    private int failMaxSize;
    
    @Value("${data-check.fail-executor.queue-capacity:500}")
    private int failQueueCapacity;

    @Bean("checkExecutor")
    public Executor checkExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("data-check-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("failWriteExecutor")
    public Executor failWriteExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(failCoreSize);
        executor.setMaxPoolSize(failMaxSize);
        executor.setQueueCapacity(failQueueCapacity);
        executor.setThreadNamePrefix("fail-write-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}