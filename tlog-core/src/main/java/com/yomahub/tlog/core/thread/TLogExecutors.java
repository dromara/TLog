package com.yomahub.tlog.core.thread;

import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import org.slf4j.MDC;

import java.util.Map;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 让异步任务继承 TLog 上下文（traceId、spanId、日志标签以及 MDC）的工具类
 * <p>
 * 用法：
 * <pre>
 * ExecutorService pool = TLogExecutors.wrap(Executors.newFixedThreadPool(8));
 * CompletableFuture.supplyAsync(supplier, TLogExecutors.wrap(executor));
 * </pre>
 * 包装后提交的任务在执行时会恢复提交线程的上下文，执行完毕后还原执行线程原有上下文。
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class TLogExecutors {

    private TLogExecutors() {
    }

    public static Executor wrap(Executor executor) {
        if (executor == null || executor instanceof TLogExecutor || executor instanceof TLogExecutorService) {
            return executor;
        }
        return new TLogExecutor(executor);
    }

    public static ExecutorService wrap(ExecutorService executorService) {
        if (executorService == null || executorService instanceof TLogExecutorService) {
            return executorService;
        }
        if (executorService instanceof ScheduledExecutorService) {
            return wrap((ScheduledExecutorService) executorService);
        }
        return new TLogExecutorService(executorService);
    }

    public static ScheduledExecutorService wrap(ScheduledExecutorService executorService) {
        if (executorService == null || executorService instanceof TLogScheduledExecutorService) {
            return executorService;
        }
        return new TLogScheduledExecutorService(executorService);
    }

    /**
     * 包装单个任务：同时传递 TTL 上下文与 MDC
     */
    public static Runnable wrap(Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        Runnable withMdc = () -> runWithMdc(mdc, runnable);
        return TtlRunnable.get(withMdc, false, true);
    }

    public static <V> Callable<V> wrap(Callable<V> callable) {
        if (callable == null) {
            return null;
        }
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        Callable<V> withMdc = () -> {
            Map<String, String> backup = MDC.getCopyOfContextMap();
            setMdc(mdc);
            try {
                return callable.call();
            } finally {
                setMdc(backup);
            }
        };
        return TtlCallable.get(withMdc, false, true);
    }

    private static void runWithMdc(Map<String, String> mdc, Runnable runnable) {
        Map<String, String> backup = MDC.getCopyOfContextMap();
        setMdc(mdc);
        try {
            runnable.run();
        } finally {
            setMdc(backup);
        }
    }

    private static void setMdc(Map<String, String> mdc) {
        if (mdc == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(mdc);
        }
    }

    private static final class TLogExecutor implements Executor {

        private final Executor delegate;

        private TLogExecutor(Executor delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(wrap(command));
        }
    }

    // AbstractExecutorService 的 submit/invokeAll/invokeAny 最终均调用 execute，
    // 保证每个入口都使用同一份 TTL + MDC 包装，且 Future 取消语义不变。
    private static class TLogExecutorService extends AbstractExecutorService {
        private final ExecutorService delegate;

        private TLogExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(wrap(command));
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
    }

    private static final class TLogScheduledExecutorService extends TLogExecutorService
            implements ScheduledExecutorService {
        private final ScheduledExecutorService scheduler;

        private TLogScheduledExecutorService(ScheduledExecutorService scheduler) {
            super(scheduler);
            this.scheduler = scheduler;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return scheduler.schedule(wrap(command), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return scheduler.schedule(wrap(callable), delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return scheduler.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return scheduler.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
        }
    }
}
