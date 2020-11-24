package com.yomahub.tlog.context;


/**
 * TLog上下文
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class TLogContext {

    private static boolean enableInvokeTimePrint = false;

    private static boolean hasTLogMDC;

    private static InheritableThreadLocal<String> traceIdTL = new InheritableThreadLocal<>();

    private static InheritableThreadLocal<String> preIvkAppTL = new InheritableThreadLocal<>();

    private static InheritableThreadLocal<String> preIvkHostTL = new InheritableThreadLocal<>();

    private static InheritableThreadLocal<String> preIpTL = new InheritableThreadLocal<>();

    public static void putTraceId(String traceId) {
        traceIdTL.set(traceId);
    }

    public static String getTraceId() {
        return traceIdTL.get();
    }

    public static void removeTraceId() {
        traceIdTL.remove();
    }

    public static void putSpanId(String spanId) {
        SpanIdGenerator.putSpanId(spanId);
    }

    public static String getSpanId() {
        return SpanIdGenerator.getSpanId();
    }

    public static void removeSpanId() {
        SpanIdGenerator.removeSpanId();
    }

    public static String getPreIvkApp() {
        return preIvkAppTL.get();
    }

    public static void putPreIvkApp(String preIvkApp) {
        preIvkAppTL.set(preIvkApp);
    }

    public static void removePreIvkApp() {
        preIvkAppTL.remove();
    }

    public static String getPreIvkHost(){
        return preIvkHostTL.get();
    }

    public static void putPreIvkHost(String preIvkHost){
        preIvkHostTL.set(preIvkHost);
    }

    public static void removePreIvkHost(){
        preIvkHostTL.remove();
    }

    public static String getPreIp() {
        return preIpTL.get();
    }

    public static void putPreIp(String preIp) {
        preIpTL.set(preIp);
    }

    public static void removePreIp() {
        preIpTL.remove();
    }

    public static boolean hasTLogMDC() {
        return hasTLogMDC;
    }

    public static void setHasTLogMDC(boolean hasTLogMDC) {
        TLogContext.hasTLogMDC = hasTLogMDC;
    }

    public static boolean enableInvokeTimePrint() {
        return enableInvokeTimePrint;
    }

    public static void setEnableInvokeTimePrint(boolean enableInvokeTimePrint) {
        TLogContext.enableInvokeTimePrint = enableInvokeTimePrint;
    }
}
