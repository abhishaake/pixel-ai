package com.av.pixel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "asyncTaskExecutor")
    @Primary
    public Executor getAsyncExecutor() {
              return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "taskExecutor")
    public TaskExecutor getTaskExecutor() {
        return new ConcurrentTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
