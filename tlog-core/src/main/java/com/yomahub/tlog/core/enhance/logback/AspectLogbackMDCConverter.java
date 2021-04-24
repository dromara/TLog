package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Logback的MDC转换器，这个类主要覆盖了start方向，目的是在pattern里如果配置了mdc的话，把全局mdc变量设为true
 *
 * @author Bryan.Zhang
 * @since 1.1.5
 */
public class AspectLogbackMDCConverter extends MDCConverter {

    private Field keyField;

    @Override
    public void start() {
        super.start();
        if (keyField == null) {
            keyField = ReflectUtil.getField(this.getClass(), "key");
        }
        String keyValue = (String) ReflectUtil.getFieldValue(this, keyField);
        if (StrUtil.isNotEmpty(keyValue) & keyValue.equals(TLogConstants.MDC_KEY)) {
            TLogContext.setHasTLogMDC(true);
        }
    }
}
