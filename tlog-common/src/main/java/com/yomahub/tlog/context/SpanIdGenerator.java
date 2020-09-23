package com.yomahub.tlog.context;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * spanId生成器
 * @author Bryan.Zhang
 * @since 2020/9/23
 */
public class SpanIdGenerator {

    private static InheritableThreadLocal<String> currentSpanIdTL = new InheritableThreadLocal<>();

    private static InheritableThreadLocal<Integer> spanIndex = new InheritableThreadLocal<>();

    private static String INITIAL_VALUE = "0";

    public static void putSpanId(String spanId){
        if(StringUtils.isBlank(spanId)){
            spanId = INITIAL_VALUE;
        }
        currentSpanIdTL.set(spanId);
        spanIndex.set(Integer.valueOf(INITIAL_VALUE));
    }

    public static String getSpanId(){
        return currentSpanIdTL.get();
    }

    public static void removeSpanId(){
        currentSpanIdTL.remove();
    }

    public static String generateNextSpanId(){
        //只在同一个request请求里进行线程安全操作
        synchronized (TLogContext.getTraceId()){
            String currentSpanId = TLogContext.getSpanId();
            spanIndex.set(spanIndex.get()+1);
            String nextSpanId = StrUtil.format("{}.{}", currentSpanId, spanIndex.get());
            return nextSpanId;
        }
    }
}
