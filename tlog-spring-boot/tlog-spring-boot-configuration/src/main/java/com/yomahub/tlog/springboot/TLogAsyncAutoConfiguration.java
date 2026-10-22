package com.yomahub.tlog.springboot;

import com.yomahub.tlog.core.thread.TLogExecutors;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

/**
 * 为 Spring 管理的 ThreadPoolTaskExecutor / SimpleAsyncTaskExecutor 透传上下文。
 * 保留线程池原有 TaskDecorator，并在其外层安装 TLog 上下文。
 * 非 Spring 管理的线程池仍需通过 TLogExecutors.wrap 包装。
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "async.enabled"}, havingValue = "true", matchIfMissing = true)
public class TLogAsyncAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator tLogTaskDecorator() {
        return new ContextTaskDecorator(null);
    }

    @Bean
    public static BeanPostProcessor tLogExecutorPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (!(bean instanceof ThreadPoolTaskExecutor) && !(bean instanceof SimpleAsyncTaskExecutor)) {
                    return bean;
                }
                // Spring 5 through 7 expose a setter but no getter for the existing decorator.
                Field field = ReflectionUtils.findField(bean.getClass(), "taskDecorator", TaskDecorator.class);
                if (field == null) {
                    throw new IllegalStateException("Cannot preserve TaskDecorator on " + bean.getClass().getName());
                }
                ReflectionUtils.makeAccessible(field);
                TaskDecorator existing = (TaskDecorator) ReflectionUtils.getField(field, bean);
                if (!(existing instanceof ContextTaskDecorator)) {
                    TaskDecorator combined = new ContextTaskDecorator(existing);
                    if (bean instanceof ThreadPoolTaskExecutor) {
                        ((ThreadPoolTaskExecutor) bean).setTaskDecorator(combined);
                    } else {
                        ((SimpleAsyncTaskExecutor) bean).setTaskDecorator(combined);
                    }
                }
                return bean;
            }
        };
    }

    private static final class ContextTaskDecorator implements TaskDecorator {
        private final TaskDecorator delegate;

        private ContextTaskDecorator(TaskDecorator delegate) {
            this.delegate = delegate;
        }

        @Override
        public Runnable decorate(Runnable runnable) {
            return TLogExecutors.wrap(delegate == null ? runnable : delegate.decorate(runnable));
        }
    }
}
