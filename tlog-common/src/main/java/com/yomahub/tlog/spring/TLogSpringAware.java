package com.yomahub.tlog.spring;

import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogRuntime;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

/**
 * Spring的上下文处理器
 * <p>
 * 2.0 起所有静态方法在非 Spring 环境（未注册本 Bean）下均安全降级，不会抛出空指针
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public class TLogSpringAware implements ApplicationContextAware {

    private static final String APP_NAME_KEY = "spring.application.name";

    private static volatile ApplicationContext applicationContext;

    private static volatile String appName;

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;
        appName = null;
        TLogRuntime.setAppName(getProperty(APP_NAME_KEY));
        String enabled = getProperty("tlog.enabled");
        TLogRuntime.setEnabled(enabled == null || Boolean.parseBoolean(enabled));
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        ApplicationContext context = applicationContext;
        return context == null ? null : (T) context.getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        ApplicationContext context = applicationContext;
        return context == null ? null : context.getBean(clazz);
    }

    /**
     * 按类型创建一个完成依赖注入的实例；非 Spring 环境下直接反射实例化
     */
    public static <T> T registerBean(Class<T> c) {
        ApplicationContext context = applicationContext;
        try {
            if (context == null) {
                return c.getDeclaredConstructor().newInstance();
            }
            return context.getAutowireCapableBeanFactory().createBean(c);
        } catch (Exception e) {
            throw new IllegalStateException("[TLOG]无法实例化 " + c.getName(), e);
        }
    }

    public static String getProperty(String key) {
        ApplicationContext context = applicationContext;
        if (context == null) {
            return null;
        }
        Environment environment = context.getEnvironment();
        return environment == null ? null : environment.getProperty(key);
    }

    /**
     * 当前应用名，读取 {@code spring.application.name}；未配置或非 Spring 环境时返回 {@link TLogConstants#UNKNOWN}
     */
    public static String getAppName() {
        String name = appName;
        if (name == null) {
            name = getProperty(APP_NAME_KEY);
            if (StrUtil.isBlank(name)) {
                // 容器未就绪时不缓存，避免把 UNKNOWN 永久固化
                return TLogConstants.UNKNOWN;
            }
            appName = name;
            TLogRuntime.setAppName(name);
        }
        return name;
    }

    static void reset() {
        applicationContext = null;
        appName = null;
        TLogRuntime.setAppName(null);
        TLogRuntime.setEnabled(true);
    }
}
