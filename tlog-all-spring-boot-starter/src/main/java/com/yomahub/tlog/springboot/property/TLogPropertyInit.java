package com.yomahub.tlog.springboot.property;

import com.yomahub.tlog.context.TLogLabelGenerator;
import org.springframework.beans.factory.InitializingBean;

public class TLogPropertyInit implements InitializingBean {

    private String pattern;

    public TLogPropertyInit(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new TLogLabelGenerator().setLabelPattern(pattern);
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
