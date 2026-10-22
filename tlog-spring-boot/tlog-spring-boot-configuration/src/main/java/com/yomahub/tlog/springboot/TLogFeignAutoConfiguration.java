package com.yomahub.tlog.springboot;

import com.yomahub.tlog.feign.filter.TLogFeignFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TLog Feign 配置的springboot自动装配类
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"feign.RequestInterceptor", "com.yomahub.tlog.feign.filter.TLogFeignFilter"})
public class TLogFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TLogFeignFilter.class)
    public TLogFeignFilter tLogFeignFilter() {
        return new TLogFeignFilter();
    }
}
