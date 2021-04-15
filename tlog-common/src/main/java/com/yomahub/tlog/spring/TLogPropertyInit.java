package com.yomahub.tlog.spring;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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

    private Boolean enableInvokeTimePrint;

    private String idGenerator;

    private Boolean mdcEnable;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StrUtil.isNotBlank(pattern)){
            TLogLabelGenerator.setLabelPattern(pattern);
        }

        if (ObjectUtil.isNotNull(enableInvokeTimePrint)){
            TLogContext.setEnableInvokeTimePrint(enableInvokeTimePrint);
        }

        if (StrUtil.isNotBlank(idGenerator)){
            try{
                TLogIdGenerator tLogIdGenerator = (TLogIdGenerator)TLogSpringAware.registerBean(Class.forName(idGenerator));
                TLogIdGeneratorLoader.setIdGenerator(tLogIdGenerator);
            }catch (Exception e){
                throw new RuntimeException("Id生成器包路径不正确");
            }
        }
        //当且仅当用户设置为true时，才修改context里的mdc属性 不影响原先AspectLogbackMDCConverter中，当自定义pattern存在tl时，开启MDC
        if (BooleanUtil.isTrue(mdcEnable)) {
            TLogContext.setHasTLogMDC(true);
        }
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Boolean getEnableInvokeTimePrint() {
        return enableInvokeTimePrint;
    }

    public void setEnableInvokeTimePrint(Boolean enableInvokeTimePrint) {
        this.enableInvokeTimePrint = enableInvokeTimePrint;
    }

    public String getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(String idGenerator) {
        this.idGenerator = idGenerator;
    }

    public Boolean getMdcEnable() {
        return mdcEnable;
    }

    public void setMdcEnable(Boolean mdcEnable) {
        this.mdcEnable = mdcEnable;
    }
}
