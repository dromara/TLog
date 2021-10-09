package com.yomahub.tlog.context;

/**
 * TLog的日志标签生成器
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class TLogLabelGenerator {

    public static String labelPattern = "<$spanId><$traceId>";

    public static String generateTLogLabel(String preApp, String preHost, String preIp, String currIp, String traceId, String spanId){
        return labelPattern.replace("$preApp",preApp)
                .replace("$preHost",preHost)
                .replace("$preIp",preIp)
                .replace("$currIp", currIp)
                .replace("$traceId",traceId)
                .replace("$spanId",spanId);
    }

    public static String getLabelPattern() {
        return labelPattern;
    }

    public static void setLabelPattern(String labelPattern) {
        TLogLabelGenerator.labelPattern = labelPattern;
    }
}
