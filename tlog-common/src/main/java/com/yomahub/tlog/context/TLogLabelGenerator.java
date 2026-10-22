package com.yomahub.tlog.context;

import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;

/**
 * TLog的日志标签生成器
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class TLogLabelGenerator {

    public static final String DEFAULT_LABEL_PATTERN = "<$spanId><$traceId>";

    public static volatile String labelPattern = DEFAULT_LABEL_PATTERN;

    public static String generateTLogLabel(String preApp, String preHost, String preIp, String currIp, String traceId, String spanId) {
        return labelPattern.replace("$preApp", nullToUnknown(preApp))
                .replace("$preHost", nullToUnknown(preHost))
                .replace("$preIp", nullToUnknown(preIp))
                .replace("$currIp", nullToUnknown(currIp))
                .replace("$traceId", nullToUnknown(traceId))
                .replace("$spanId", nullToUnknown(spanId));
    }

    private static String nullToUnknown(String value) {
        return StrUtil.isBlank(value) ? TLogConstants.UNKNOWN : value;
    }

    public static String getLabelPattern() {
        return labelPattern;
    }

    public static void setLabelPattern(String labelPattern) {
        TLogLabelGenerator.labelPattern = StrUtil.isBlank(labelPattern) ? DEFAULT_LABEL_PATTERN : labelPattern;
    }
}
