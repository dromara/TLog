package com.yomahub.tlog.springboot;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TLogExecutorConfigurationTest {
    @AfterEach
    void clean() {
        new TLogRPCHandler().cleanThreadLocal();
    }

    @Test
    void customPoolBeanPropagatesAfterWorkerHasBeenReused() {
        new ApplicationContextRunner().withUserConfiguration(PlainPool.class)
                .withConfiguration(AutoConfigurations.of(TLogAsyncAutoConfiguration.class))
                .run(context -> {
                    ThreadPoolTaskExecutor pool = context.getBean(ThreadPoolTaskExecutor.class);
                    assertThat(pool.submit(TLogContext::getTraceId).get()).isNull();
                    TLogContext.putTraceId("first");
                    assertThat(pool.submit(TLogContext::getTraceId).get()).isEqualTo("first");
                    TLogContext.putTraceId("second");
                    assertThat(pool.submit(TLogContext::getTraceId).get()).isEqualTo("second");
                    TLogContext.removeTraceId();
                    assertThat(pool.submit(TLogContext::getTraceId).get()).isNull();
                });
    }

    @Test
    void existingDecoratorsRunOnceInsidePropagatedContext() {
        new ApplicationContextRunner().withUserConfiguration(DecoratedPools.class)
                .withConfiguration(AutoConfigurations.of(TLogAsyncAutoConfiguration.class))
                .run(context -> {
                    Probe probe = context.getBean(Probe.class);
                    ThreadPoolTaskExecutor pool = context.getBean(ThreadPoolTaskExecutor.class);
                    pool.submit(() -> {}).get();
                    int before = probe.runs.get();
                    TLogContext.putTraceId("pool-trace");
                    assertThat(pool.submit(TLogContext::getTraceId).get()).isEqualTo("pool-trace");
                    assertThat(probe.runs.get()).isEqualTo(before + 1);
                    assertThat(probe.trace.get()).isEqualTo("pool-trace");

                    TLogContext.putTraceId("simple-trace");
                    CompletableFuture<String> result = new CompletableFuture<>();
                    context.getBean(SimpleAsyncTaskExecutor.class).execute(() -> result.complete(TLogContext.getTraceId()));
                    assertThat(result.get()).isEqualTo("simple-trace");
                    assertThat(probe.runs.get()).isEqualTo(before + 2);
                    assertThat(probe.trace.get()).isEqualTo("simple-trace");
                });
    }

    @Test
    void disabledAsyncLeavesCustomPoolUntouched() {
        new ApplicationContextRunner().withUserConfiguration(PlainPool.class)
                .withConfiguration(AutoConfigurations.of(TLogAsyncAutoConfiguration.class))
                .withPropertyValues("tlog.async.enabled=false")
                .run(context -> {
                    ThreadPoolTaskExecutor pool = context.getBean(ThreadPoolTaskExecutor.class);
                    pool.submit(() -> {}).get();
                    TLogContext.putTraceId("caller");
                    assertThat(pool.submit(TLogContext::getTraceId).get()).isNull();
                });
    }

    @Configuration
    static class PlainPool {
        @Bean
        ThreadPoolTaskExecutor customPool() {
            ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
            pool.setCorePoolSize(1);
            pool.setMaxPoolSize(1);
            return pool;
        }
    }

    static class Probe {
        final AtomicInteger runs = new AtomicInteger();
        final AtomicReference<String> trace = new AtomicReference<>();
    }

    @Configuration
    static class DecoratedPools {
        @Bean
        Probe probe() {
            return new Probe();
        }

        @Bean
        TaskDecorator userDecorator(Probe probe) {
            return task -> () -> {
                probe.runs.incrementAndGet();
                probe.trace.set(TLogContext.getTraceId());
                task.run();
            };
        }

        @Bean
        ThreadPoolTaskExecutor pool(TaskDecorator userDecorator) {
            ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
            pool.setCorePoolSize(1);
            pool.setMaxPoolSize(1);
            pool.setTaskDecorator(userDecorator);
            return pool;
        }

        @Bean
        SimpleAsyncTaskExecutor simple(TaskDecorator userDecorator) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
            executor.setTaskDecorator(userDecorator);
            return executor;
        }
    }
}
