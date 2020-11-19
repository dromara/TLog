package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.PatternLayoutEncoderBase;

/**
 * 基于日志适配方式的logback自定义encoder
 *
 * @author Bryan.Zhang
 * @since 1.0.0
 */
public class AspectLogbackEncoder extends PatternLayoutEncoderBase<ILoggingEvent> {
    @Override
    public void start() {
        PatternLayout patternLayout = new AspectLogbackLayout();
        patternLayout.setContext(context);
        patternLayout.setPattern(getPattern());
        patternLayout.setOutputPatternAsHeader(outputPatternAsHeader);
        patternLayout.start();
        this.layout = patternLayout;
        super.start();
    }
}
