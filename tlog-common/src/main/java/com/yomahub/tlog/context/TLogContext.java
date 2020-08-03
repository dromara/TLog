package com.yomahub.tlog.context;

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
}
