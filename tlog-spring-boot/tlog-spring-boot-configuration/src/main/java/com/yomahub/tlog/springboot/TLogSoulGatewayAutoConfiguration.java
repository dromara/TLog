package com.yomahub.tlog.springboot;

import com.yomahub.tlog.webflux.aop.SoulPluginLogAop;
import com.yomahub.tlog.webflux.filter.TLogWebFluxFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Soul 网关的自动装配类
 *
 * @author naah
 * @since 1.3.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "web.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.dromara.soul.web.handler.SoulWebHandler", "com.yomahub.tlog.webflux.aop.SoulPluginLogAop"})
public class TLogSoulGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TLogWebFluxFilter.class)
    public TLogWebFluxFilter traceFilter(@Value("${tlog.web.response-header:true}") boolean responseHeader) {
        TLogWebFluxFilter filter = new TLogWebFluxFilter();
        filter.setResponseHeader(responseHeader);
        filter.setForwardHeaders(true);
        return filter;
    }

    @Bean
    @ConditionalOnMissingBean(SoulPluginLogAop.class)
    public SoulPluginLogAop pluginAop() {
        return new SoulPluginLogAop();
    }
}
