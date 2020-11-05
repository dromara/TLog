package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import org.slf4j.MDC;

import java.lang.reflect.Field;

public class AspectLogbackMDCConverter extends MDCConverter {

    private Field keyField;

    @Override
    public void start() {
        super.start();
        if(keyField == null){
            keyField = ReflectUtil.getField(this.getClass(),"key");
        }
        String keyValue = (String)ReflectUtil.getFieldValue(this,keyField);
        if(StrUtil.isNotEmpty(keyValue) & keyValue.equals(TLogConstants.MDC_KEY)){
            TLogContext.setHasTLogMDC(true);
        }
    }
}
