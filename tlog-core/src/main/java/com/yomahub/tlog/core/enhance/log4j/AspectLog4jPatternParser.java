package com.yomahub.tlog.core.enhance.log4j;

import org.apache.log4j.helpers.PatternParser;

/**
 * 基于日志适配模式的Log4j日志框架的模板自定义Parser
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
public class AspectLog4jPatternParser extends PatternParser {
    public AspectLog4jPatternParser(String pattern) {
        super(pattern);
    }

    @Override
    protected void finalizeConverter(char c) {
        switch (c) {
            case 'm':
                addConverter(new AspectLog4jPatternConverter());
                break;
            case 'X':
                String xOpt = extractOption();
                addConverter(new AspectLog4jMDCPatternConverter(formattingInfo, xOpt));
                break;
            default:
                super.finalizeConverter(c);
                break;
        }
    }
}
