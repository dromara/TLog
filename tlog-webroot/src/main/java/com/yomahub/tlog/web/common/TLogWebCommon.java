package com.yomahub.tlog.web.common;

import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.TLogContextSnapshot;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.core.util.TLogJsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Function;

/**
 * TLog web这块的逻辑封装类
 * <p>
 * 本类的核心方法只依赖 header 读取函数，与 servlet 命名空间无关，javax 与 jakarta 两套 Filter 共用；
 * {@link #preHandle(HttpServletRequest)} 仅为兼容 1.x 的 javax 调用方保留，jakarta 环境下不会被调用，也不会触发 javax 类加载。
 * <p>
 * 同一请求内重复进入（例如同时注册了 Filter 与拦截器）时只在最外层建立和清理上下文。
 *
 * @author Bryan.Zhang
 * @since 1.1.5
 */
public class TLogWebCommon extends TLogRPCHandler {

    public static final String REQUEST_CONTEXT_ATTRIBUTE = "com.yomahub.tlog.web.CONTEXT";

    private static final Logger log = LoggerFactory.getLogger(TLogWebCommon.class);

    private static final TLogWebCommon INSTANCE = new TLogWebCommon();

    /**
     * 当前线程的嵌套深度，不需要向子线程传递
     */
    private static final ThreadLocal<Entry> ENTRY = new ThreadLocal<>();

    private static final class Entry {
        private int depth = 1;
        private final TLogContextSnapshot.Scope scope;

        private Entry(TLogContextSnapshot.Scope scope) {
            this.scope = scope;
        }
    }

    public static TLogWebCommon loadInstance() {
        return INSTANCE;
    }

    /**
     * 兼容 1.x 的 javax 入口
     */
    public void preHandle(HttpServletRequest request) {
        preHandle(request::getHeader);
    }

    public void preHandle(Function<String, String> headerReader) {
        preHandle(headerReader, null);
    }

    /** Restore a request's context when a servlet container dispatches it again. */
    public void preHandle(Function<String, String> headerReader, TLogContextSnapshot requestContext) {
        Entry entry = ENTRY.get();
        if (entry != null) {
            entry.depth++;
            return;
        }
        // A container thread may inherit or retain an unrelated request's TTL state.
        // The outermost web dispatch owns these keys; do not resurrect them on exit.
        // Nested filters/interceptors above still preserve their active outer scope.
        cleanThreadLocal();
        TLogContextSnapshot.Scope scope = (requestContext == null
                ? TLogContextSnapshot.capture() : requestContext).restore();
        ENTRY.set(new Entry(scope));
        try {
            if (requestContext == null) {
                processProviderSide(headerReader);
            }
        } catch (RuntimeException | Error failure) {
            ENTRY.remove();
            scope.close();
            throw failure;
        }
    }

    public void afterCompletion() {
        Entry entry = ENTRY.get();
        if (entry == null || --entry.depth > 0) {
            return;
        }
        ENTRY.remove();
        entry.scope.close();
    }

    /**
     * 开启耗时打印时记录请求开始
     *
     * @return 开始时间（纳秒），未开启时返回 -1
     */
    public long beforeInvoke(String uri, Map<String, String[]> parameterMap) {
        if (!TLogContext.enableInvokeTimePrint()) {
            return -1L;
        }
        if (log.isInfoEnabled()) {
            log.info("[TLOG]开始请求URL[{}],参数为:{}", uri, TLogJsonUtil.toJsonForLog(parameterMap));
        }
        return System.nanoTime();
    }

    public void afterInvoke(String uri, long startNanos) {
        if (startNanos >= 0) {
            log.info("[TLOG]结束URL[{}]的调用,耗时为:{}毫秒", uri, (System.nanoTime() - startNanos) / 1_000_000);
        }
    }
}
