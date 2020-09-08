package com.yomahub.tlog.core.enhance.log4j;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

/**
 * @author Bryan.Zhang
 */
public class AspectLog4jPatternLayout extends PatternLayout {
    @Override
    protected PatternParser createPatternParser(String pattern) {
        return new AspectLog4jPatternParser(pattern);
    }
}
