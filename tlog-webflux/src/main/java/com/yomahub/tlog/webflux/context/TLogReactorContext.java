package com.yomahub.tlog.webflux.context;

import com.yomahub.tlog.core.context.TLogContextSnapshot;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import com.yomahub.tlog.context.SpanIdGenerator;
import com.yomahub.tlog.context.TLogContext;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.function.Function;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reactor 3.1+ 的 TLog 上下文桥接。上下文属于订阅，每个回调只临时恢复线程状态。
 * 不依赖不同 Reactor 版本间已移除的 subscriberContext / 新增的 contextWrite API。
 */
public final class TLogReactorContext {

    private static final String HOOK_KEY = "com.yomahub.tlog.context";
    private static final Object CONTEXT_KEY = TLogContextSnapshot.class;
    // Reactor 线程可能在请求作用域内首次创建，TTL 会自动把当时的请求复制到新线程。
    // 识别这种继承，避免把它误当作工作线程原有的上下文，在回调结束时重新留下。
    private static final InheritableThreadLocal<ScopeOwner> SCOPE_OWNER = new InheritableThreadLocal<ScopeOwner>() {
        @Override
        protected ScopeOwner childValue(ScopeOwner parent) {
            return parent == null ? null : new ScopeOwner(parent.snapshot);
        }
    };
    private static boolean installed;

    private TLogReactorContext() {
    }

    /** 安装独立命名的 hook，不替换应用的其他 hook。 */
    public static synchronized void install() {
        if (!installed) {
            Hooks.onEachOperator(HOOK_KEY, publisher -> {
                if (publisher instanceof Mono) {
                    return publisher instanceof Fuseable ? new FuseableScopedMono<>((Mono<Object>) publisher)
                            : new ScopedMono<>((Mono<Object>) publisher);
                }
                if (publisher instanceof ConnectableFlux) {
                    return publisher instanceof Fuseable ? new FuseableScopedConnectableFlux<>((ConnectableFlux<Object>) publisher)
                            : new ScopedConnectableFlux<>((ConnectableFlux<Object>) publisher);
                }
                if (publisher instanceof Flux) {
                    return publisher instanceof Fuseable ? new FuseableScopedFlux<>((Flux<Object>) publisher)
                            : new ScopedFlux<>((Flux<Object>) publisher);
                }
                // ParallelFlux 的 rail 订阅 API 在 Reactor 3.1 为 protected；使用
                // Reactor 自己的 lift 保留 ParallelFlux 类型并包装每条 rail 的回调。
                return Operators.<Object, Object>lift((source, actual) -> {
                    Context context = actual.currentContext();
                    return context.hasKey(CONTEXT_KEY)
                            ? new ScopedSubscriber<>(actual, context, context.get(CONTEXT_KEY)) : actual;
                }).apply(publisher);
            });
            installed = true;
        }
    }

    /** 为一个订阅设置请求快照；不会改变组装线程上的上下文。 */
    public static <T> Mono<T> withContext(Mono<T> source, TLogContextSnapshot snapshot) {
        install();
        return new Mono<T>() {
            @Override
            public void subscribe(CoreSubscriber<? super T> actual) {
                subscribeScoped(source, actual, actual.currentContext().put(CONTEXT_KEY, snapshot));
            }
        };
    }

    /** 在订阅时读取 Reactor 上下文；没有响应式上下文时沿用当前调用线程。 */
    public static <T> Mono<T> defer(Function<TLogContextSnapshot, Mono<T>> factory) {
        install();
        return new Mono<T>() {
            @Override
            public void subscribe(CoreSubscriber<? super T> actual) {
                Context context = actual.currentContext();
                TLogContextSnapshot snapshot = context.hasKey(CONTEXT_KEY)
                        ? context.get(CONTEXT_KEY) : TLogContextSnapshot.capture();
                try (Scope ignored = open(snapshot)) {
                    Mono<T> source;
                    try {
                        source = Objects.requireNonNull(factory.apply(snapshot), "TLog reactive factory returned null");
                    } catch (Throwable error) {
                        Exceptions.throwIfFatal(error);
                        Operators.error(actual, error);
                        return;
                    }
                    subscribeScoped(source, actual, context.put(CONTEXT_KEY, snapshot));
                }
            }
        };
    }

    private static <T> void subscribeScoped(Publisher<T> source, CoreSubscriber<? super T> actual,
                                             Context context) {
        if (!context.hasKey(CONTEXT_KEY)) {
            subscribe(source, actual);
            return;
        }
        TLogContextSnapshot snapshot = context.get(CONTEXT_KEY);
        try (Scope ignored = open(snapshot)) {
            subscribe(source, new ScopedSubscriber<>(actual, context, snapshot));
        }
    }

    private static <T> void subscribe(Publisher<T> source, CoreSubscriber<? super T> actual) {
        // 使用 CoreSubscriber 重载，避免重新进入全局 onLastOperator hook。
        if (source instanceof Mono) {
            ((Mono<T>) source).subscribe(actual);
        } else if (source instanceof Flux) {
            ((Flux<T>) source).subscribe(actual);
        } else {
            source.subscribe(actual);
        }
    }

    private static Scope open(TLogContextSnapshot snapshot) {
        ScopeOwner owner = SCOPE_OWNER.get();
        if (owner != null && owner.thread != Thread.currentThread()) {
            // 子线程可能已经显式建立了自己的请求。只清理由父作用域自动继承、
            // 且至今未被替换的状态；不能删除该线程后来创建的独立请求。
            if (owner.matchesCurrentRequest()) {
                new TLogRPCHandler().cleanThreadLocal();
            }
            SCOPE_OWNER.remove();
            owner = null;
        }
        if (owner != null && owner.snapshot == snapshot && owner.matchesCurrentRequest()) {
            return Scope.NOOP;
        }
        TLogContextSnapshot.Scope restored = snapshot.restore();
        SCOPE_OWNER.set(new ScopeOwner(snapshot));
        return new Scope(restored, owner);
    }

    private static final class ScopeOwner {
        private final Thread thread = Thread.currentThread();
        private final String traceId = TLogContext.getTraceId();
        private final String spanId = TLogContext.getSpanId();
        private final AtomicInteger spanIndex = SpanIdGenerator.spanIndex.get();
        private final TLogContextSnapshot snapshot;

        private ScopeOwner(TLogContextSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        private boolean matchesCurrentRequest() {
            return spanIndex == SpanIdGenerator.spanIndex.get()
                    && Objects.equals(traceId, TLogContext.getTraceId())
                    && Objects.equals(spanId, TLogContext.getSpanId());
        }
    }

    private static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null, null);
        private final TLogContextSnapshot.Scope context;
        private final ScopeOwner previousOwner;

        private Scope(TLogContextSnapshot.Scope context, ScopeOwner previousOwner) {
            this.context = context;
            this.previousOwner = previousOwner;
        }

        @Override
        public void close() {
            if (context == null) {
                return;
            }
            try {
                context.close();
            } finally {
                if (previousOwner == null) {
                    SCOPE_OWNER.remove();
                } else {
                    SCOPE_OWNER.set(previousOwner);
                }
            }
        }
    }

    private static class ScopedMono<T> extends Mono<T> implements Scannable {
        private final Mono<T> source;

        private ScopedMono(Mono<T> source) {
            this.source = source;
        }

        @Override
        public void subscribe(CoreSubscriber<? super T> actual) {
            subscribeScoped(source, actual, actual.currentContext());
        }

        @Override
        public Object scanUnsafe(Attr key) {
            return key == Attr.PARENT ? source : Scannable.from(source).scanUnsafe(key);
        }
    }

    private static class ScopedFlux<T> extends Flux<T> implements Scannable {
        private final Flux<T> source;

        private ScopedFlux(Flux<T> source) {
            this.source = source;
        }

        @Override
        public void subscribe(CoreSubscriber<? super T> actual) {
            subscribeScoped(source, actual, actual.currentContext());
        }

        @Override
        public Object scanUnsafe(Attr key) {
            return key == Attr.PARENT ? source : Scannable.from(source).scanUnsafe(key);
        }
    }

    private static class ScopedConnectableFlux<T> extends ConnectableFlux<T> implements Scannable {
        private final ConnectableFlux<T> source;

        private ScopedConnectableFlux(ConnectableFlux<T> source) {
            this.source = source;
        }

        @Override
        public void connect(Consumer<? super Disposable> cancelSupport) {
            source.connect(cancelSupport);
        }

        @Override
        public void subscribe(CoreSubscriber<? super T> actual) {
            subscribeScoped(source, actual, actual.currentContext());
        }

        @Override
        public Object scanUnsafe(Attr key) {
            return key == Attr.PARENT ? source : Scannable.from(source).scanUnsafe(key);
        }
    }

    // 无 TLog Context 时直接传递原订阅，保留原有的 operator fusion 和诊断链。
    private static final class FuseableScopedMono<T> extends ScopedMono<T> implements Fuseable {
        private FuseableScopedMono(Mono<T> source) { super(source); }
    }

    private static final class FuseableScopedFlux<T> extends ScopedFlux<T> implements Fuseable {
        private FuseableScopedFlux(Flux<T> source) { super(source); }
    }

    private static final class FuseableScopedConnectableFlux<T> extends ScopedConnectableFlux<T> implements Fuseable {
        private FuseableScopedConnectableFlux(ConnectableFlux<T> source) { super(source); }
    }

    private static final class ScopedSubscriber<T> implements CoreSubscriber<T> {
        private final CoreSubscriber<? super T> actual;
        private final Context context;
        private final TLogContextSnapshot snapshot;

        private ScopedSubscriber(CoreSubscriber<? super T> actual, Context context,
                                 TLogContextSnapshot snapshot) {
            this.actual = actual;
            this.context = context;
            this.snapshot = snapshot;
        }

        @Override
        public Context currentContext() {
            return context;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            try (Scope ignored = open(snapshot)) {
                actual.onSubscribe(new Fuseable.QueueSubscription<T>() {
                    // 跟踪请求的回调是上下文边界，禁止跨边界融合；非跟踪订阅不会走到此处。
                    @Override
                    public int requestFusion(int requestedMode) { return Fuseable.NONE; }

                    @Override
                    public T poll() { throw new UnsupportedOperationException("TLog scope does not fuse"); }

                    @Override
                    public int size() { return 0; }

                    @Override
                    public boolean isEmpty() { return true; }

                    @Override
                    public void clear() { }

                    @Override
                    public void request(long n) {
                        try (Scope ignored = open(snapshot)) {
                            subscription.request(n);
                        }
                    }

                    @Override
                    public void cancel() {
                        try (Scope ignored = open(snapshot)) {
                            subscription.cancel();
                        }
                    }
                });
            }
        }

        @Override
        public void onNext(T value) {
            try (Scope ignored = open(snapshot)) {
                actual.onNext(value);
            }
        }

        @Override
        public void onError(Throwable error) {
            try (Scope ignored = open(snapshot)) {
                actual.onError(error);
            }
        }

        @Override
        public void onComplete() {
            try (Scope ignored = open(snapshot)) {
                actual.onComplete();
            }
        }
    }
}
