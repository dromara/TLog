package com.yomahub.tlog.core.aop;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Maps;
import com.yomahub.tlog.core.annotation.TLogAspect;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.core.convert.AspectLogConvert;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;

/**
 * 自定义埋点注解切面，用于拦截@AspectLogAop
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
@Aspect
public class AspectLogAop {

    private static final Logger log = LoggerFactory.getLogger(AspectLogAop.class);

    @Pointcut("@annotation(com.yomahub.tlog.core.annotation.TLogAspect)")
    public void cut() {
    }

    @Around("cut()")
    public Object around(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = signature.getParameterNames();
        Map<String, Object> paramNameValueMap = Maps.newHashMap();
        for (int i = 0; i < parameterNames.length; i++) {
            paramNameValueMap.put(parameterNames[i], args[i]);
        }

        TLogAspect tlogAspect = method.getAnnotation(TLogAspect.class);
        String[] aspectExpressions = tlogAspect.value();
        String pattern = tlogAspect.pattern().replaceAll("\\{\\}", "{0}");
        String joint = tlogAspect.joint();
        Class<? extends AspectLogConvert> convertClazz = tlogAspect.convert();

        StringBuilder sb = new StringBuilder();
        if (!convertClazz.equals(AspectLogConvert.class)) {
            AspectLogConvert convert = convertClazz.newInstance();
            try {
                sb.append(convert.convert(args));
            } catch (Throwable t) {
                log.error("[AspectLog]some errors happens in AspectLog's convert", t);
            }
        } else {
            for (String aspectExpression : aspectExpressions) {
                String aspLogValueItem = getExpressionValue(aspectExpression, paramNameValueMap);
                if (StringUtils.isNotBlank(aspLogValueItem)) {
                    sb.append(StrUtil.format("{}:{}", aspectExpression, aspLogValueItem));
                    sb.append(joint);
                }
            }
        }

        String aspLogValue = sb.toString();
        if (StringUtils.isNotBlank(aspLogValue)) {
            aspLogValue = aspLogValue.substring(0, aspLogValue.length() - 1);

            aspLogValue = MessageFormat.format(pattern, aspLogValue);

            //拿到之前的标签
            String currentLabel = AspectLogContext.getLogValue();
            AspectLogContext.putLogValue(currentLabel + aspLogValue);
        }

        try {
            return jp.proceed();
        } finally {
            AspectLogContext.remove();
        }
    }

    private String getExpressionValue(String expression, Object o) {
        String[] expressionItems = expression.split("\\.");
        for (String item : expressionItems) {
            if (String.class.isAssignableFrom(o.getClass())) {
                return (String) o;
            } else if (Integer.class.isAssignableFrom(o.getClass())) {
                return ((Integer) o).toString();
            } else if (Long.class.isAssignableFrom(o.getClass())) {
                return ((Long) o).toString();
            } else if (Double.class.isAssignableFrom(o.getClass())) {
                return ((Double) o).toString();
            } else if (BigDecimal.class.isAssignableFrom(o.getClass())) {
                return ((BigDecimal) o).toPlainString();
            } else if (Date.class.isAssignableFrom(o.getClass())) {
                return DateUtil.formatDateTime((Date) o);
            } else if (Map.class.isAssignableFrom(o.getClass())) {
                Object v = ((Map) o).get(item);
                if (v == null) {
                    return null;
                }
                return getExpressionValue(getRemainExpression(expression, item), v);
            } else {
                try {
                    Object v = MethodUtils.invokeMethod(o, "get" + item.substring(0, 1).toUpperCase() + item.substring(1));
                    if (v == null) {
                        return null;
                    }
                    return getExpressionValue(getRemainExpression(expression, item), v);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private String getRemainExpression(String expression, String expressionItem) {
        if (expression.equals(expressionItem)) {
            return expressionItem;
        } else {
            return expression.substring(expressionItem.length() + 1);
        }
    }
}
