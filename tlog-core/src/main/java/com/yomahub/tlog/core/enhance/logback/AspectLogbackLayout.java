package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.PatternLayout;

/**
 * 基于日志适配方式logback的自定义layout
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class AspectLogbackLayout extends PatternLayout {
    static {
        defaultConverterMap.put("m", AspectLogbackConverter.class.getName());
        defaultConverterMap.put("msg", AspectLogbackConverter.class.getName());
        defaultConverterMap.put("message", AspectLogbackConverter.class.getName());
    }
}
