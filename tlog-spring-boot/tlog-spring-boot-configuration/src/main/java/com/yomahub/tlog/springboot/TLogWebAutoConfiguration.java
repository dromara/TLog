package com.yomahub.tlog.springboot;

import com.yomahub.tlog.springboot.property.TLogProperty;
import com.yomahub.tlog.web.filter.TLogJakartaServletFilter;
import com.yomahub.tlog.web.filter.TLogServletFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.EnumSet;

/**
 * TLog web层面的自动装配类（Servlet 栈）
 * <p>
 * 同一个 jar 同时覆盖 Spring Boot 2.x（javax.servlet）与 3.x / 4.x（jakarta.servlet）：
 * 以是否存在 Spring 6 引入的 {@code org.springframework.aot.AotDetector} 判断 Spring 代际，
 * 只注册与当前容器命名空间匹配的过滤器；即便 classpath 上同时存在两套 servlet-api 也不会误注册。
 * <p>
 * 显式映射 REQUEST / ASYNC / ERROR。setFilter 的参数随 servlet 命名空间变化，
 * 通过运行期类型查找这个公开方法，其余调用使用跨版本一致的签名。
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "tlog", name = {"enabled", "web.enabled"}, havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TLogProperty.class)
public class TLogWebAutoConfiguration {

    static final String SPRING_6_MARKER = "org.springframework.aot.AotDetector";

    @Configuration
    @ConditionalOnClass(name = {"javax.servlet.Filter", "com.yomahub.tlog.web.filter.TLogServletFilter"})
    @ConditionalOnMissingClass(SPRING_6_MARKER)
    static class JavaxServletConfiguration {

        @Bean
        @ConditionalOnMissingBean(TLogServletFilter.class)
        public TLogServletFilter tLogServletFilter(TLogProperty property) {
            TLogServletFilter filter = new TLogServletFilter();
            filter.setOrder(property.getWeb().getOrder());
            filter.setResponseHeaderEnabled(property.getWeb().isResponseHeader());
            filter.setAsyncContextEnabled(property.getAsync().isEnabled());
            return filter;
        }

        @Bean
        @ConditionalOnMissingBean(name = "tLogServletFilterRegistration")
        public FilterRegistrationBean<?> tLogServletFilterRegistration(TLogServletFilter filter) {
            return register(filter, filter.getOrder(), javax.servlet.Filter.class,
                    EnumSet.of(javax.servlet.DispatcherType.REQUEST, javax.servlet.DispatcherType.ASYNC,
                            javax.servlet.DispatcherType.ERROR));
        }
    }

    @Configuration
    @ConditionalOnClass(name = {"jakarta.servlet.Filter", SPRING_6_MARKER,
            "com.yomahub.tlog.web.filter.TLogJakartaServletFilter"})
    static class JakartaServletConfiguration {

        @Bean
        @ConditionalOnMissingBean(TLogJakartaServletFilter.class)
        public TLogJakartaServletFilter tLogJakartaServletFilter(TLogProperty property) {
            TLogJakartaServletFilter filter = new TLogJakartaServletFilter();
            filter.setOrder(property.getWeb().getOrder());
            filter.setResponseHeaderEnabled(property.getWeb().isResponseHeader());
            filter.setAsyncContextEnabled(property.getAsync().isEnabled());
            return filter;
        }

        @Bean
        @ConditionalOnMissingBean(name = "tLogJakartaServletFilterRegistration")
        public FilterRegistrationBean<?> tLogJakartaServletFilterRegistration(TLogJakartaServletFilter filter) {
            return register(filter, filter.getOrder(), jakarta.servlet.Filter.class,
                    EnumSet.of(jakarta.servlet.DispatcherType.REQUEST, jakarta.servlet.DispatcherType.ASYNC,
                            jakarta.servlet.DispatcherType.ERROR));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static FilterRegistrationBean<?> register(Object filter, int order, Class<?> filterType,
                                                       EnumSet dispatcherTypes) {
        FilterRegistrationBean<?> registration = new FilterRegistrationBean<>();
        Method setFilter = ReflectionUtils.findMethod(FilterRegistrationBean.class, "setFilter", filterType);
        if (setFilter == null) {
            throw new IllegalStateException("Unsupported Servlet FilterRegistrationBean API: " + filterType.getName());
        }
        ReflectionUtils.invokeMethod(setFilter, registration, filter);
        registration.setName("tlogFilter");
        registration.setOrder(order);
        registration.setAsyncSupported(true);
        registration.setDispatcherTypes(dispatcherTypes);
        return registration;
    }
}
