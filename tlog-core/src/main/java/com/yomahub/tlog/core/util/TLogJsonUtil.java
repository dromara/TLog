package com.yomahub.tlog.core.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

/**
 * 日志打印用的 JSON 工具：序列化失败不会影响业务调用
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class TLogJsonUtil {

    /**
     * 单次打印的最大长度，避免大对象撑爆日志
     */
    private static final int MAX_LENGTH = 2048;

    private TLogJsonUtil() {
    }

    public static String toJsonForLog(Object value) {
        if (value == null) {
            return "null";
        }
        String json;
        try {
            json = JSON.toJSONString(value, JSONWriter.Feature.ReferenceDetection, JSONWriter.Feature.IgnoreErrorGetter);
        } catch (Throwable t) {
            return "<" + value.getClass().getName() + " 无法序列化: " + t.getClass().getSimpleName() + ">";
        }
        if (json != null && json.length() > MAX_LENGTH) {
            return json.substring(0, MAX_LENGTH) + "...(" + json.length() + " chars)";
        }
        return json;
    }
}
