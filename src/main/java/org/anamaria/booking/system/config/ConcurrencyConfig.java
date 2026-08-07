package org.anamaria.booking.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ConcurrencyConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService bookingExecutor(
            @Value("${concurrency.executor.core-pool-size:8}") int corePoolSize,
            @Value("${concurrency.executor.max-pool-size:32}") int maxPoolSize,
            @Value("${concurrency.executor.queue-capacity:200}") int queueCapacity) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("booking-executor-" + t.threadId());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService virtualThreadBookingExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
