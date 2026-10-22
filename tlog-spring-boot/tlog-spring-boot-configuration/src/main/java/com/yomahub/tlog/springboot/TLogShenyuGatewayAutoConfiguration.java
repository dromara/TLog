package com.yomahub.tlog.springboot;

import com.yomahub.tlog.webflux.aop.ShenyuPluginLogAop;
import com.yomahub.tlog.webflux.filter.TLogWebFluxFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ShenYu 网关的自动装配类
 * <p>
 * Copy from @author naah
 *
 * @author timgise
 * @since 1.5.0
 */
@Configuration
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "web.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.apache.shenyu.web.handler.ShenyuWebHandler", "com.yomahub.tlog.webflux.aop.ShenyuPluginLogAop"})
public class TLogShenyuGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TLogWebFluxFilter.class)
    public TLogWebFluxFilter traceFilter(@Value("${tlog.web.response-header:true}") boolean responseHeader) {
        TLogWebFluxFilter filter = new TLogWebFluxFilter();
        filter.setResponseHeader(responseHeader);
        filter.setForwardHeaders(true);
        return filter;
    }

    @Bean
    @ConditionalOnMissingBean(ShenyuPluginLogAop.class)
    public ShenyuPluginLogAop pluginLogAop() {
        return new ShenyuPluginLogAop();
    }
}
