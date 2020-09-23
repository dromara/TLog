package com.yomahub.tlog.context;

/**
 * TLog的日志标签生成器
 * @author Bryan.Zhang
 * @since 2020/9/15
 */
public class TLogLabelGenerator {

    public static String labelPattern = "<$spanId><$traceId>";

    public static String generateTLogLabel(String preApp, String preIp, String traceId, String spanId){
        return labelPattern.replace("$preApp",preApp)
                .replace("$preIp",preIp)
                .replace("$traceId",traceId)
                .replace("$spanId",spanId);
    }

    public String getLabelPattern() {
        return labelPattern;
    }

    public void setLabelPattern(String labelPattern) {
        TLogLabelGenerator.labelPattern = labelPattern;
    }
}
