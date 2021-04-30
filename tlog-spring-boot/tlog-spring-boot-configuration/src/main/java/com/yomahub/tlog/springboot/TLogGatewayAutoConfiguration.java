package com.yomahub.tlog.springboot;

import com.yomahub.tlog.gateway.filter.TLogGatewayFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zs
 * @since 1.2.4-BETA1
 */
@Configuration
@ConditionalOnClass(name = {"org.springframework.cloud.gateway.filter.GlobalFilter"})
public class TLogGatewayAutoConfiguration {

    @Bean
    public TLogGatewayFilter tLogGatewayFilter(){
        return new TLogGatewayFilter();
    }
}
