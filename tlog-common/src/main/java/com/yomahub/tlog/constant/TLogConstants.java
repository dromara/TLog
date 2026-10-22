package com.yomahub.tlog.constant;

/**
 * 静态变量类
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class TLogConstants {

    public static final String TLOG_TRACE_KEY = "tlogTraceId";

    public static final String TLOG_SPANID_KEY = "tlogSpanId";

    public static final String CURR_IP_KEY = "currIp";

    public static final String PRE_IVK_APP_KEY = "preIvkApp";

    public static final String PRE_IVK_APP_HOST = "preIvkHost";

    public static final String PRE_IP_KEY = "preIp";

    public static final String UNKNOWN = "NONE";

    public static final String MDC_KEY = "tl";

    public static final String WEBFLUX_EXCHANGE = "exchange";

    /**
     * W3C Trace Context 标准头，见 https://www.w3.org/TR/trace-context/
     */
    public static final String W3C_TRACE_PARENT = "traceparent";

    /**
     * TLog 写入 MDC 的全部 key，清理时使用
     */
    public static final String[] MDC_KEYS = {MDC_KEY, TLOG_TRACE_KEY, TLOG_SPANID_KEY, CURR_IP_KEY,
            PRE_IP_KEY, PRE_IVK_APP_HOST, PRE_IVK_APP_KEY};

}
