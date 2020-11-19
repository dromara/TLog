package com.yomahub.tlog.core.enhance.log4j;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

/**
 * 基于日志适配模式的log4j自定义pattern layout
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class AspectLog4jPatternLayout extends PatternLayout {
    @Override
    protected PatternParser createPatternParser(String pattern) {
        return new AspectLog4jPatternParser(pattern);
    }
}
