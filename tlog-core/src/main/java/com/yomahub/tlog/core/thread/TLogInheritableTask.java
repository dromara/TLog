package com.yomahub.tlog.core.thread;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.MDC;

import java.util.Map;

/**
 * 线程池中使用TLog标签的增强支持类,用来解决用普通的Runnable接口可能traceId等标签无效的情况
 * <p>
 * 2.0 起基于 TTL 的 capture/replay/restore 实现，不再反射访问 {@code Thread.inheritableThreadLocals}，
 * 可在 JDK 17+ 强封装环境下正常使用；任务执行完毕后恢复执行线程原有的上下文，不会污染线程池中的线程。
 * 更推荐使用 {@link TLogExecutors} 直接包装线程池。
 *
 * @author Bryan.Zhang
 * @since 1.0.3
 */
public abstract class TLogInheritableTask implements Runnable {

    private final Object captured = TransmittableThreadLocal.Transmitter.capture();

    private final Map<String, String> capturedMdc = MDC.getCopyOfContextMap();

    public abstract void runTask();

    @Override
    public final void run() {
        Object backup = TransmittableThreadLocal.Transmitter.replay(captured);
        Map<String, String> backupMdc = MDC.getCopyOfContextMap();
        if (capturedMdc != null) {
            MDC.setContextMap(capturedMdc);
        } else {
            MDC.clear();
        }
        try {
            runTask();
        } finally {
            if (backupMdc != null) {
                MDC.setContextMap(backupMdc);
            } else {
                MDC.clear();
            }
            TransmittableThreadLocal.Transmitter.restore(backup);
        }
    }

    public void start() {
        new Thread(this).start();
    }
}
