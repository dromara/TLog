package com.yomahub.tlog.core.aop;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.InstructionSet;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.core.annotation.TLogAspect;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.core.convert.AspectLogConvert;
import com.yomahub.tlog.exception.TLogCustomLabelExpressionException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义埋点注解切面，用于拦截 {@link TLogAspect}
 * <p>
 * 方法执行期间在当前标签后追加自定义标签，方法返回后恢复原标签，嵌套调用不会累加
 *
 * @author Bryan.Zhang
 * @since 1.1.0
 */
@Aspect
public class AspectLogAop {

    private static final Logger log = LoggerFactory.getLogger(AspectLogAop.class);

    private final ExpressRunner expressRunner = new ExpressRunner();

    private final Map<Class<? extends AspectLogConvert>, AspectLogConvert> convertCache = new ConcurrentHashMap<>();

    private final DefaultParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();

    @Pointcut("@annotation(com.yomahub.tlog.core.annotation.TLogAspect)")
    public void cut() {
    }

    @Around("cut()")
    public Object around(ProceedingJoinPoint jp) throws Throwable {
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = BridgeMethodResolver.findBridgedMethod(
                AopUtils.getMostSpecificMethod(signature.getMethod(), jp.getTarget().getClass()));
        TLogAspect tlogAspect = method.getAnnotation(TLogAspect.class);
        if (tlogAspect == null) {
            tlogAspect = signature.getMethod().getAnnotation(TLogAspect.class);
        }
        String[] names = parameterNames.getParameterNames(method);
        String aspLogValue = tlogAspect == null ? null : buildLabel(tlogAspect,
                names == null ? signature.getParameterNames() : names, jp.getArgs());

        if (StrUtil.isBlank(aspLogValue)) {
            return jp.proceed();
        }

        String previousLabel = AspectLogContext.getLogValue();
        String previousMdc = MDC.get(TLogConstants.MDC_KEY);
        String newLabel = StrUtil.isBlank(previousLabel) ? aspLogValue : previousLabel + " " + aspLogValue;
        AspectLogContext.putLogValue(newLabel);
        MDC.put(TLogConstants.MDC_KEY, newLabel);
        try {
            return jp.proceed();
        } finally {
            if (previousLabel == null) {
                AspectLogContext.remove();
            } else {
                AspectLogContext.putLogValue(previousLabel);
            }
            if (previousMdc == null) {
                MDC.remove(TLogConstants.MDC_KEY);
            } else {
                MDC.put(TLogConstants.MDC_KEY, previousMdc);
            }
        }
    }

    private String buildLabel(TLogAspect tlogAspect, String[] parameterNames, Object[] args) {
        String joint = tlogAspect.joint();
        List<String> parts = new ArrayList<>();

        //处理字符串类型标签
        if (StrUtil.isNotBlank(tlogAspect.str())) {
            parts.add(tlogAspect.str());
        }

        //处理自定义converter
        Class<? extends AspectLogConvert> convertClazz = tlogAspect.convert();
        if (!AspectLogConvert.class.equals(convertClazz)) {
            try {
                String converted = getConvert(convertClazz).convert(args);
                if (StrUtil.isNotBlank(converted)) {
                    parts.add(converted);
                }
            } catch (Throwable t) {
                log.error("[AspectLog]some errors happens in AspectLog's convert", t);
            }
        }

        //处理表达式
        String[] expressions = tlogAspect.value();
        if (expressions.length > 0) {
            Map<String, Object> paramNameValueMap = new HashMap<>();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                    paramNameValueMap.put(parameterNames[i], args[i]);
                }
            }
            for (String expression : expressions) {
                String value = getExpressionValue(expression, paramNameValueMap);
                if (StrUtil.isNotBlank(value)) {
                    parts.add(expression + ":" + value);
                }
            }
        }

        if (parts.isEmpty()) {
            return null;
        }
        return StrUtil.format(tlogAspect.pattern(), String.join(joint, parts));
    }

    private AspectLogConvert getConvert(Class<? extends AspectLogConvert> clazz) {
        return convertCache.computeIfAbsent(clazz, c -> {
            try {
                return c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("[AspectLog]无法实例化转换器 " + c.getName(), e);
            }
        });
    }

    private String getExpressionValue(String expression, Map<String, Object> map) {
        List<String> errorList = new ArrayList<>();
        try {
            InstructionSet instructionSet = expressRunner.getInstructionSetFromLocalCache("map." + expression);
            DefaultContext<String, Object> context = new DefaultContext<>();
            context.put("map", map);
            Object value = expressRunner.execute(instructionSet, context, errorList, false, false);

            if (ObjectUtil.isNull(value)) {
                return null;
            }
            if (ObjectUtil.isBasicType(value) || value instanceof CharSequence) {
                return value.toString();
            }
            return JSON.toJSONString(value);
        } catch (Throwable t) {
            for (String scriptErrorMsg : errorList) {
                log.error("\n{}", scriptErrorMsg);
            }
            throw new TLogCustomLabelExpressionException("[AspectLog]表达式[" + expression + "]执行失败: " + t.getMessage(), t);
        }
    }
}
