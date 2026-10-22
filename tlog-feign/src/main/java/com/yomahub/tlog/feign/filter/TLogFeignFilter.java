package com.yomahub.tlog.feign.filter;

import com.yomahub.tlog.context.TLogPropagator;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * feign的拦截器
 * <p>
 * 应用名由 {@link com.yomahub.tlog.spring.TLogSpringAware#getAppName()} 提供，未配置 {@code spring.application.name} 时不再导致启动失败
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class TLogFeignFilter implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        TLogPropagator.inject((name, value) -> {
            // 先清空再写入，避免重试时同名 header 重复追加；不传值调用 header(name) 即清空，该行为在 feign 9 ~ 13 中一致
            requestTemplate.header(name);
            requestTemplate.header(name, value);
        });
    }
}
