package com.yomahub.tlog.spring;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.id.TLogIdGenerator;
import com.yomahub.tlog.id.TLogIdGeneratorLoader;
import org.springframework.beans.factory.InitializingBean;

/**
 * TLog参数初始化类，适用于springboot和spring
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public class TLogPropertyInit implements InitializingBean {

    private String pattern;

    private boolean enableInvokeTimePrint;

    private String idGenerator;

    @Override
    public void afterPropertiesSet() throws Exception {
        TLogLabelGenerator.setLabelPattern(pattern);
        TLogContext.setEnableInvokeTimePrint(enableInvokeTimePrint);

        TLogIdGenerator tLogIdGenerator = (TLogIdGenerator)TLogSpringAware.registerBean(Class.forName(idGenerator));
        TLogIdGeneratorLoader.setIdGenerator(tLogIdGenerator);
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

    public String getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(String idGenerator) {
        this.idGenerator = idGenerator;
    }
}
