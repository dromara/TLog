package com.yomahub.tlog.id;

import java.util.Map;

/**
 * traceId 生成器，用户可继承此类自定义 traceId 规则
 * <p>
 * TLog 通过 {@link #generate(Map)} 调用单例生成器：调用期间串行保护旧版
 * {@link #extData} 字段，返回或抛异常时恢复外层调用的数据并释放请求引用。
 * 新子类应通过 {@link #getExtData()} 读取当前调用的扩展数据。
 *
 * @author Bryan.Zhang
 * @since 1.3.0
 */
public abstract class TLogIdGenerator {

    private final ThreadLocal<Map<String, Object>> extDataLocal = new ThreadLocal<>();

    /**
     * 当前线程本次生成 traceId 时的扩展数据，可能为 null
     *
     * @deprecated 请改用 {@link #getExtData()}。通过 {@link #generate(Map)} 调用时兼容旧子类；
     * 直接并发调用 withExtData(...).generateTraceId() 不具备原子性。
     */
    @Deprecated
    protected volatile Map<String, Object> extData;

    abstract public String generateTraceId();

    public synchronized TLogIdGenerator withExtData(Map<String, Object> extData) {
        setExtData(extData);
        this.extData = extData;
        return this;
    }

    /**
     * 当前线程本次生成 traceId 时的扩展数据
     */
    protected Map<String, Object> getExtData() {
        return extDataLocal.get();
    }

    /**
     * 生成 traceId 并清理线程内扩展数据，TLog 内部统一通过该方法调用
     */
    public final synchronized String generate(Map<String, Object> extData) {
        Map<String, Object> previous = extDataLocal.get();
        Map<String, Object> previousField = this.extData;
        try {
            withExtData(extData);
            return generateTraceId();
        } finally {
            setExtData(previous);
            this.extData = previousField;
        }
    }

    private void setExtData(Map<String, Object> data) {
        if (data == null) {
            extDataLocal.remove();
        } else {
            extDataLocal.set(data);
        }
    }
}
