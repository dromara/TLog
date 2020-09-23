package com.yomahub.tlog.context;

import org.apache.commons.lang3.StringUtils;

/**
 * TLog上下文
 * @author Bryan.Zhang
 * @since 2020/9/11
 */
public class TLogContext {

    private static InheritableThreadLocal<String> traceIdTL = new InheritableThreadLocal<>();

    public static void putTraceId(String traceId){
        traceIdTL.set(traceId);
    }

    public static String getTraceId(){
        return traceIdTL.get();
    }

    public static void removeTraceId(){
        traceIdTL.remove();
    }

    public static void putSpanId(String spanId){
        SpanIdGenerator.putSpanId(spanId);
    }

    public static String getSpanId(){
        return SpanIdGenerator.getSpanId();
    }

    public static void removeSpanId(){
        SpanIdGenerator.removeSpanId();
    }
}
