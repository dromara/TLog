package com.yomahub.tlog.springboot;

import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.context.TLogRuntime;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * TLog 基础组件的自动装配类
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
@Configuration
public class TLogCommonAutoConfiguration {

    /** Initialize the switch before any application singleton or @PostConstruct callback. */
    @Bean
    public static BeanFactoryPostProcessor tLogRuntimeInitializer(Environment environment) {
        return beanFactory -> {
            String enabled = environment.getProperty("tlog.enabled");
            TLogRuntime.setEnabled(enabled == null || Boolean.parseBoolean(enabled));
            TLogRuntime.setAppName(environment.getProperty("spring.application.name"));
        };
    }

    /**
     * 静态工厂避免提前实例化配置类；使用方通过显式 Bean 依赖保证初始化顺序
     */
    @Bean
    @ConditionalOnMissingBean(TLogSpringAware.class)
    public static TLogSpringAware tLogSpringAware() {
        return new TLogSpringAware();
    }
}
