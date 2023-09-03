package com.example.transactionaltestintegration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ServerConfig implements AsyncConfigurer {

    @Bean
    @Primary
    public TaskExecutor defaultTaskExecutor() {
        return buildThreadPoolTaskExecutor();
    }

    private ThreadPoolTaskExecutor buildThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setThreadNamePrefix("transaction");
        return taskExecutor;
    }
}
