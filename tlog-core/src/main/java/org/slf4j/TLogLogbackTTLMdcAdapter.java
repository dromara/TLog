package org.slf4j;

import org.slf4j.spi.MDCAdapter;
import java.util.Deque;
import java.util.Map;

/**
 * 针对于Logback日志的mdc adapter。
 *
 * @author Bryan.Zhang
 * @since 1.3.7
 * @deprecated 2.0 起迁移到 {@link com.yomahub.tlog.core.enhance.logback.TLogLogbackTTLMdcAdapter}，
 * 保留 1.x 的构造、实例方法及静态工厂；新代码应使用新包名
 */
@Deprecated
public class TLogLogbackTTLMdcAdapter implements MDCAdapter {

    private static final TLogLogbackTTLMdcAdapter INSTANCE = new TLogLogbackTTLMdcAdapter(
            com.yomahub.tlog.core.enhance.logback.TLogLogbackTTLMdcAdapter.getInstance());

    private final com.yomahub.tlog.core.enhance.logback.TLogLogbackTTLMdcAdapter delegate;

    public TLogLogbackTTLMdcAdapter() {
        this(new com.yomahub.tlog.core.enhance.logback.TLogLogbackTTLMdcAdapter());
    }

    private TLogLogbackTTLMdcAdapter(com.yomahub.tlog.core.enhance.logback.TLogLogbackTTLMdcAdapter delegate) {
        this.delegate = delegate;
    }

    public static MDCAdapter getInstance() {
        com.yomahub.tlog.core.enhance.logback.TLogLogbackTTLMdcAdapter.install();
        return INSTANCE;
    }

    @Override
    public void put(String key, String value) {
        delegate.put(key, value);
    }

    @Override
    public String get(String key) {
        return delegate.get(key);
    }

    @Override
    public void remove(String key) {
        delegate.remove(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    public Map<String, String> getPropertyMap() {
        return delegate.getPropertyMap();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return delegate.getCopyOfContextMap();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setContextMap(Map contextMap) {
        delegate.setContextMap(contextMap);
    }

    public void pushByKey(String key, String value) {
        delegate.pushByKey(key, value);
    }

    public String popByKey(String key) {
        return delegate.popByKey(key);
    }

    public Deque<String> getCopyOfDequeByKey(String key) {
        return delegate.getCopyOfDequeByKey(key);
    }

    public void clearDequeByKey(String key) {
        delegate.clearDequeByKey(key);
    }
}
