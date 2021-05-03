package com.yomahub.tlog.springboot;

import com.yomahub.tlog.webflux.aop.SoulPluginLogAop;
import com.yomahub.tlog.webflux.filter.TLogWebFluxFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author naah
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(name = {"org.dromara.soul.web.handler.SoulWebHandler"})
public class TLogSoulGatewayAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public TLogWebFluxFilter traceFilter() {
        return new TLogWebFluxFilter();
    }

    @Bean
    public SoulPluginLogAop pluginAop(){
        return new SoulPluginLogAop();
    }

}

