package com.example.delegationsecurity.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class Config {

    @Bean
    public TaskExecutor userThreadPool() {
        ThreadPoolTaskExecutor taskExecutor = buildThreadPoolTaskExecutor(1, "e-2");
        taskExecutor.setTaskDecorator(runnable -> new DelegatingSecurityContextRunnable(runnable, SecurityContextHolder.getContext()));

        return taskExecutor;
    }

    private ThreadPoolTaskExecutor buildThreadPoolTaskExecutor(int coreSize, String threadNamePrefix) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

        taskExecutor.setCorePoolSize(coreSize);
        taskExecutor.setMaxPoolSize(coreSize);
        taskExecutor.setThreadNamePrefix(threadNamePrefix);
        return taskExecutor;
    }
}
