package com.strataguard.service.notification;

import com.strataguard.core.config.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.UUID;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notif-");
        executor.setTaskDecorator(tenantContextDecorator());
        executor.initialize();
        return executor;
    }

    private TaskDecorator tenantContextDecorator() {
        return runnable -> {
            UUID tenantId = TenantContext.getTenantId();
            return () -> {
                try {
                    if (tenantId != null) {
                        TenantContext.setTenantId(tenantId);
                    }
                    runnable.run();
                } finally {
                    TenantContext.clear();
                }
            };
        };
    }
}
