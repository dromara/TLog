package com.yomahub.tlog.spring;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogLabelGenerator;
import com.yomahub.tlog.id.TLogDefaultIdGenerator;
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

    /**
     * 直接指定的 ID 生成器实例（例如容器中的 Bean），优先级高于 {@link #idGenerator} 类名
     */
    private TLogIdGenerator idGeneratorInstance;

    @Override
    public void afterPropertiesSet() {
        if (StrUtil.isNotBlank(pattern)) {
            TLogLabelGenerator.setLabelPattern(pattern);
        }

        if (ObjectUtil.isNotNull(enableInvokeTimePrint)) {
            TLogContext.setEnableInvokeTimePrint(enableInvokeTimePrint);
        }

        TLogIdGenerator generator = resolveIdGenerator();
        if (generator != null) {
            TLogIdGeneratorLoader.setIdGenerator(generator);
        }

        //当且仅当用户设置为true时，才修改context里的mdc属性 不影响原先AspectLogbackMDCConverter中，当自定义pattern存在tl时，开启MDC
        if (BooleanUtil.isTrue(mdcEnable)) {
            TLogContext.setHasTLogMDC(true);
        }
    }

    private TLogIdGenerator resolveIdGenerator() {
        if (idGeneratorInstance != null) {
            return idGeneratorInstance;
        }
        if (StrUtil.isBlank(idGenerator) || TLogDefaultIdGenerator.class.getName().equals(idGenerator.trim())) {
            return null;
        }
        Class<?> clazz;
        try {
            clazz = ClassUtil.loadClass(idGenerator.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("[TLOG]Id生成器类不存在: " + idGenerator, e);
        }
        if (!TLogIdGenerator.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("[TLOG]Id生成器必须继承 " + TLogIdGenerator.class.getName() + ": " + idGenerator);
        }
        return (TLogIdGenerator) TLogSpringAware.registerBean(clazz);
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

    public TLogIdGenerator getIdGeneratorInstance() {
        return idGeneratorInstance;
    }

    public void setIdGeneratorInstance(TLogIdGenerator idGeneratorInstance) {
        this.idGeneratorInstance = idGeneratorInstance;
    }
}
