package com.yomahub.tlog.springboot;

import com.yomahub.tlog.webflux.filter.TLogWebFluxFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 普通 WebFlux 应用的自动装配类
 * <p>
 * Spring Cloud Gateway 由 {@link TLogGatewayAutoConfiguration} 处理，Soul / ShenYu 由各自的装配类注册，此处排除这些场景避免重复建立上下文
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "web.enabled"}, havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "com.yomahub.tlog.webflux.filter.TLogWebFluxFilter")
@ConditionalOnMissingClass({"org.springframework.cloud.gateway.filter.GlobalFilter",
        "org.dromara.soul.web.handler.SoulWebHandler",
        "org.apache.shenyu.web.handler.ShenyuWebHandler"})
public class TLogWebFluxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TLogWebFluxFilter.class)
    public TLogWebFluxFilter tLogWebFluxFilter(@Value("${tlog.web.response-header:true}") boolean responseHeader) {
        TLogWebFluxFilter filter = new TLogWebFluxFilter();
        filter.setResponseHeader(responseHeader);
        return filter;
    }
}
