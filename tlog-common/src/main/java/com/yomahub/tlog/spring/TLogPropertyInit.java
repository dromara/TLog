package com.yomahub.tlog.spring;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import org.springframework.beans.factory.InitializingBean;

/**
 * TLog参数初始化类，适用于springboot和spring
 *
 * @author Bryan.Zhang
 * @since 1.1.6
 */
public class TLogPropertyInit implements InitializingBean {

    private String pattern;

    private boolean enableInvokeTimePrint;

    @Override
    public void afterPropertiesSet() throws Exception {
        TLogLabelGenerator.setLabelPattern(pattern);
        TLogContext.setEnableInvokeTimePrint(enableInvokeTimePrint);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean getEnableInvokeTimePrint() {
        return enableInvokeTimePrint;
    }

    public void setEnableInvokeTimePrint(boolean enableInvokeTimePrint) {
        this.enableInvokeTimePrint = enableInvokeTimePrint;
    }
}
