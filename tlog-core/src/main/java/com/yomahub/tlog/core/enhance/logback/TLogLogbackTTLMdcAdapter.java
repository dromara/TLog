package com.yomahub.tlog.core.enhance.logback;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于 TransmittableThreadLocal 的 MDC 适配器，让 MDC 内容随 TTL 传递到子线程 / 线程池
 * <p>
 * 兼容 slf4j 1.7 / 2.x 与 logback 1.2 ~ 1.5：
 * <ul>
 *     <li>slf4j 1.7 的静态字段为 {@code MDC.mdcAdapter}，2.x 改名为 {@code MDC.MDC_ADAPTER}</li>
 *     <li>logback 1.3+ 的 {@code LoggingEvent} 从 {@code LoggerContext.mdcAdapter} 读取 MDC，而不是 {@code MDC} 类，需要一并替换</li>
 * </ul>
 * 采用写时复制：每次修改都替换为新的 Map，已传递给子线程的快照不会被父线程后续修改影响。
 *
 * @author Bryan.Zhang
 * @since 1.3.7
 */
public class TLogLogbackTTLMdcAdapter implements MDCAdapter {

    private static final TLogLogbackTTLMdcAdapter INSTANCE = new TLogLogbackTTLMdcAdapter();

    private static volatile boolean installed;

    private final TransmittableThreadLocal<Map<String, String>> contextMap = new TransmittableThreadLocal<>();

    /**
     * slf4j 2.x 的 push/pop 栈，按线程隔离，不向子线程传递
     */
    private final ThreadLocal<Map<String, Deque<String>>> dequeMap = new ThreadLocal<>();

    public static TLogLogbackTTLMdcAdapter getInstance() {
        return INSTANCE;
    }

    /**
     * 把本适配器安装到 slf4j 的 MDC 以及当前 slf4j 绑定的 logback LoggerContext 上
     * <p>
     * 注意：不能在 logback 配置过程中调用（此时 slf4j 尚未完成初始化，拿到的不是真正的 LoggerContext），
     * logback 配置期间请使用 {@link #install(Object)}
     *
     * @return 是否安装成功
     */
    public static boolean install() {
        return install(LoggerFactory.getILoggerFactory());
    }

    /**
     * 把本适配器安装到 slf4j 的 MDC 以及指定的 logback LoggerContext 上
     *
     * @param loggerContext logback 的 {@code LoggerContext}；logback 1.3+ 的日志事件从它读取 MDC，1.2 没有该字段，忽略即可
     * @return 是否安装成功
     */
    public static synchronized boolean install(Object loggerContext) {
        boolean mdcInstalled = installed || setStaticField(MDC.class, INSTANCE, "MDC_ADAPTER", "mdcAdapter");
        if (loggerContext instanceof ILoggerFactory) {
            setInstanceField(loggerContext, INSTANCE, "mdcAdapter");
        }
        installed = mdcInstalled;
        return mdcInstalled;
    }

    private static boolean setStaticField(Class<?> clazz, Object value, String... candidates) {
        for (String name : candidates) {
            try {
                Field field = clazz.getDeclaredField(name);
                if (!Modifier.isStatic(field.getModifiers()) || !field.getType().isInstance(value)) {
                    continue;
                }
                field.setAccessible(true);
                field.set(null, value);
                return true;
            } catch (NoSuchFieldException ignored) {
                // 尝试下一个候选字段名
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private static void setInstanceField(Object target, Object value, String name) {
        for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                if (Modifier.isStatic(field.getModifiers()) || !field.getType().isInstance(value)) {
                    return;
                }
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // 继续查找父类
            } catch (Exception e) {
                return;
            }
        }
    }

    @Override
    public void put(String key, String val) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }
        Map<String, String> oldMap = contextMap.get();
        Map<String, String> newMap = oldMap == null ? new HashMap<>() : new HashMap<>(oldMap);
        newMap.put(key, val);
        contextMap.set(Collections.unmodifiableMap(newMap));
    }

    @Override
    public String get(String key) {
        Map<String, String> map = contextMap.get();
        return map == null || key == null ? null : map.get(key);
    }

    @Override
    public void remove(String key) {
        if (key == null) {
            return;
        }
        Map<String, String> oldMap = contextMap.get();
        if (oldMap == null || !oldMap.containsKey(key)) {
            return;
        }
        if (oldMap.size() == 1) {
            contextMap.remove();
            return;
        }
        Map<String, String> newMap = new HashMap<>(oldMap);
        newMap.remove(key);
        contextMap.set(Collections.unmodifiableMap(newMap));
    }

    @Override
    public void clear() {
        contextMap.remove();
        dequeMap.remove();
    }

    /**
     * 供 logback 1.2 的 {@code LoggingEvent} 以外的组件读取只读视图
     */
    public Map<String, String> getPropertyMap() {
        return contextMap.get();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        Map<String, String> map = contextMap.get();
        return map == null ? null : new HashMap<>(map);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setContextMap(Map contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            this.contextMap.remove();
            return;
        }
        this.contextMap.set(Collections.unmodifiableMap(new HashMap<String, String>(contextMap)));
    }

    // ---------------- slf4j 2.x 新增方法（在 slf4j 1.7 下是普通方法，不影响编译与运行） ----------------

    public void pushByKey(String key, String value) {
        if (key == null) {
            return;
        }
        Map<String, Deque<String>> map = dequeMap.get();
        if (map == null) {
            map = new HashMap<>();
            dequeMap.set(map);
        }
        map.computeIfAbsent(key, k -> new ArrayDeque<>()).push(value);
    }

    public String popByKey(String key) {
        Map<String, Deque<String>> map = dequeMap.get();
        if (map == null || key == null) {
            return null;
        }
        Deque<String> deque = map.get(key);
        return deque == null ? null : deque.pollFirst();
    }

    public Deque<String> getCopyOfDequeByKey(String key) {
        Map<String, Deque<String>> map = dequeMap.get();
        if (map == null || key == null) {
            return null;
        }
        Deque<String> deque = map.get(key);
        return deque == null ? null : new ArrayDeque<>(deque);
    }

    public void clearDequeByKey(String key) {
        Map<String, Deque<String>> map = dequeMap.get();
        if (map != null && key != null) {
            Deque<String> deque = map.get(key);
            if (deque != null) {
                deque.clear();
            }
        }
    }
}
