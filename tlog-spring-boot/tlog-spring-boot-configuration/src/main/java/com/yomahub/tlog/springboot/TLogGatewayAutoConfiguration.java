package com.yomahub.tlog.springboot;

import com.yomahub.tlog.gateway.filter.TLogGatewayFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud Gateway 的自动装配类
 *
 * @author zs
 * @since 1.2.4-BETA1
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "web.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.cloud.gateway.filter.GlobalFilter", "com.yomahub.tlog.gateway.filter.TLogGatewayFilter"})
public class TLogGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TLogGatewayFilter.class)
    public TLogGatewayFilter tLogGatewayFilter(@Value("${tlog.web.response-header:true}") boolean responseHeader) {
        return new TLogGatewayFilter(responseHeader);
    }
}
