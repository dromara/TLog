package com.yomahub.tlog.springboot.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * tlog的springboot配置参数
 * <p>
 * 1.x 的 {@code tlog.pattern}、{@code tlog.enableInvokeTimePrint}、{@code tlog.idGenerator}、{@code tlog.mdcEnable}
 * 保持不变（Spring Boot 宽松绑定同时支持 {@code tlog.enable-invoke-time-print} 写法）
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "tlog", ignoreUnknownFields = true)
public class TLogProperty {

    /**
     * 是否启用 TLog 自动埋点；关闭后仅保留读取开关和应用名的基础配置
     */
    private boolean enabled = true;

    /**
     * 日志标签模板，可用占位符：$preApp、$preHost、$preIp、$currIp、$traceId、$spanId
     */
    private String pattern;

    /**
     * 是否打印 web / dubbo 调用的参数与耗时
     */
    private boolean enableInvokeTimePrint;

    /**
     * 自定义 traceId 生成器的全类名，需继承 com.yomahub.tlog.id.TLogIdGenerator；也可以直接声明一个该类型的 Bean
     */
    private String idGenerator;

    /**
     * 是否以 MDC 方式输出标签（日志 pattern 中使用 %X{tl}）
     */
    private Boolean mdcEnable;

    /**
     * 入站没有 TLog 头时，是否沿用 W3C traceparent 头中的 traceId
     */
    private boolean w3cTraceContext = true;

    private final Web web = new Web();

    private final Async async = new Async();

    private final HttpClient httpClient = new HttpClient();

    private final Scheduled scheduled = new Scheduled();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean enableInvokeTimePrint() {
        return enableInvokeTimePrint;
    }

    public boolean getEnableInvokeTimePrint() {
        return enableInvokeTimePrint;
    }

    public void setEnableInvokeTimePrint(boolean enableInvokeTimePrint) {
        this.enableInvokeTimePrint = enableInvokeTimePrint;
    }

    public String getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(String idGenerator) {
        this.idGenerator = idGenerator;
    }

    public Boolean getMdcEnable() {
        return mdcEnable;
    }

    public void setMdcEnable(Boolean mdcEnable) {
        this.mdcEnable = mdcEnable;
    }

    public boolean isW3cTraceContext() {
        return w3cTraceContext;
    }

    public void setW3cTraceContext(boolean w3cTraceContext) {
        this.w3cTraceContext = w3cTraceContext;
    }

    public Web getWeb() {
        return web;
    }

    public Async getAsync() {
        return async;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public Scheduled getScheduled() {
        return scheduled;
    }

    public static class Web {

        /**
         * 是否注册 Servlet 过滤器 / WebFlux 过滤器
         */
        private boolean enabled = true;

        /**
         * 是否把 traceId 写入响应头 tlogTraceId
         */
        private boolean responseHeader = true;

        /**
         * Servlet 过滤器顺序，默认在字符编码过滤器之后、安全过滤器之前
         */
        // Literal keeps the configuration processor's default value equal to the runtime value.
        private int order = -2147483647;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isResponseHeader() {
            return responseHeader;
        }

        public void setResponseHeader(boolean responseHeader) {
            this.responseHeader = responseHeader;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }
    }

    public static class Async {

        /**
         * 是否为 Spring 管理的线程池（@Async、ThreadPoolTaskExecutor 等）自动传递 TLog 上下文
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class HttpClient {

        /**
         * 是否为容器中的 RestTemplate、WebClient.Builder、RestClient.Builder 自动添加 TLog 拦截器
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Scheduled {

        /**
         * 是否为 @Scheduled 定时任务生成 traceId
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
