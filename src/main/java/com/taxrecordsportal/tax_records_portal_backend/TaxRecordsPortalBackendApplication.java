package com.taxrecordsportal.tax_records_portal_backend;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class TaxRecordsPortalBackendApplication implements AsyncConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(TaxRecordsPortalBackendApplication.class, args);
		System.out.println("Compiled Successfully! :)) ");
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(25);
		executor.setThreadNamePrefix("async-");
		executor.initialize();
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (throwable, method, params) ->
			log.error("Uncaught async exception in {}: {}", method.getName(), throwable.getMessage(), throwable);
	}

}
