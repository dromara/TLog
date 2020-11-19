package com.yomahub.tlog.springboot;

import com.yomahub.tlog.feign.filter.TLogFeignFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TLog Feign 配置的springboot自动装配类
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(name = {"feign.RequestInterceptor"})
public class TLogFeignAutoConfiguration {

    @Bean
    public TLogFeignFilter tLogFeignFilter() {
        return new TLogFeignFilter();
    }
}
