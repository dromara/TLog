package com.yomahub.tlog.webflux.context;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.context.TLogContextSnapshot;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.Disposable;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TLogReactorContextTest {

    @BeforeEach
    void setup() {
        TLogRuntime.setEnabled(true);
        new TLogRPCHandler().cleanThreadLocal();
        TLogReactorContext.install();
    }

    @AfterEach
    void clean() {
        new TLogRPCHandler().cleanThreadLocal();
    }

    private TLogContextSnapshot snapshot(String traceId) {
        TLogRPCHandler handler = new TLogRPCHandler();
        handler.processProviderSide(name -> TLogConstants.TLOG_TRACE_KEY.equals(name) ? traceId : null);
        TLogContextSnapshot snapshot = TLogContextSnapshot.capture();
        handler.cleanThreadLocal();
        return snapshot;
    }

    private void assertTrace(String trace) {
        assertEquals(trace, TLogContext.getTraceId());
        assertEquals(trace, MDC.get(TLogConstants.TLOG_TRACE_KEY));
    }

    @Test
    void interleavedSubscriptionsRestoreTheirOwnContextAndTheCallingThread() {
        TLogContextSnapshot a = snapshot("A");
        TLogContextSnapshot b = snapshot("B");
        Mono<String> first = TLogReactorContext.withContext(Mono.defer(() -> {
            assertTrace("A");
            return Mono.just(TLogContext.getTraceId());
        }), a);
        Mono<String> second = TLogReactorContext.withContext(Mono.defer(() -> {
            assertTrace("B");
            return Mono.just(TLogContext.getTraceId());
        }), b);
        TLogContext.putTraceId("caller");
        MDC.put(TLogConstants.TLOG_TRACE_KEY, "caller");
        assertEquals("A", first.block());
        assertTrace("caller");
        assertEquals("B", second.block());
        assertTrace("caller");
    }

    @Test
    void concurrentRequestsKeepContextAcrossSubscribeOnPublishOnAndDelay() throws Exception {
        Scheduler worker = Schedulers.newSingle("tlog-reactive-test");
        try {
            List<Mono<Void>> requests = new ArrayList<>();
            for (int i = 0; i < 80; i++) {
                String trace = "request-" + i;
                TLogContextSnapshot snapshot = snapshot(trace);
                Mono<Void> work = Mono.defer(() -> {
                    assertTrace(trace);
                    return Mono.just(trace);
                }).subscribeOn(worker)
                        .delayElement(Duration.ofMillis(1), worker)
                        .publishOn(worker)
                        .flatMap(value -> {
                            assertTrace(trace);
                            return Mono.fromRunnable(() -> assertTrace(trace));
                        });
                requests.add(TLogReactorContext.withContext(work, snapshot));
            }
            Flux.merge(requests).blockLast(Duration.ofSeconds(10));
            CompletableFuture<String> after = new CompletableFuture<>();
            worker.schedule(() -> after.complete(TLogContext.getTraceId()));
            assertNull(after.get(5, TimeUnit.SECONDS), "首次在请求中创建的工作线程也不能残留继承的 trace");
            assertNull(TLogContext.getTraceId());
        } finally {
            worker.dispose();
        }
    }

    @Test
    void errorAndCancellationCallbacksHaveContextAndRestoreTheirCaller() {
        TLogContextSnapshot snapshot = snapshot("request");
        AtomicReference<String> errorTrace = new AtomicReference<>();
        RuntimeException failure = new IllegalStateException("business failure");
        Mono<Void> failed = TLogReactorContext.withContext(Mono.<Void>defer(() -> {
            assertTrace("request");
            throw failure;
        }).doOnError(error -> errorTrace.set(TLogContext.getTraceId())), snapshot);
        assertSame(failure, assertThrows(IllegalStateException.class, failed::block));
        assertEquals("request", errorTrace.get());
        assertNull(TLogContext.getTraceId());

        AtomicReference<String> cancelled = new AtomicReference<>();
        Disposable subscription = TLogReactorContext.withContext(Mono.<Void>never()
                .doOnCancel(() -> cancelled.set(TLogContext.getTraceId())), snapshot).subscribe();
        TLogContext.putTraceId("canceller");
        subscription.dispose();
        assertEquals("request", cancelled.get());
        assertEquals("canceller", TLogContext.getTraceId());
    }

    @Test
    void hooksPreserveConnectableAndParallelPublishers() {
        TLogContextSnapshot snapshot = snapshot("parallel");
        Mono<List<Integer>> work = Flux.range(1, 20).publish().refCount()
                .parallel(2).runOn(Schedulers.parallel())
                .map(value -> {
                    assertTrace("parallel");
                    return value;
                }).sequential().collectList();
        assertEquals(20, TLogReactorContext.withContext(work, snapshot)
                .block(Duration.ofSeconds(5)).size());
        assertEquals(Integer.valueOf(6), Flux.just(1, 2, 3).publish().autoConnect().reduce(Integer::sum).block());
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void aChildThreadsExplicitRequestSurvivesAnInheritedScopeMarker() throws Exception {
        TLogContextSnapshot inherited = snapshot("A");
        CompletableFuture<String> result = new CompletableFuture<>();
        TLogReactorContext.withContext(Mono.fromRunnable(() -> {
            Thread child = new Thread(() -> {
                TLogRPCHandler handler = new TLogRPCHandler();
                try {
                    handler.processProviderSide(name -> TLogConstants.TLOG_TRACE_KEY.equals(name) ? "B" : null);
                    TLogReactorContext.withContext(Mono.fromRunnable(() -> assertTrace("A")), inherited).block();
                    assertTrace("B");
                    result.complete(TLogContext.getTraceId());
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                } finally {
                    handler.cleanThreadLocal();
                }
            });
            child.start();
        }), inherited).block();
        assertEquals("B", result.get(5, TimeUnit.SECONDS));
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void fatalErrorsAreNotConvertedToRecoverableReactiveSignals() {
        LinkageError fatal = new LinkageError("fatal linkage failure");
        assertSame(fatal, assertThrows(LinkageError.class, () -> TLogReactorContext.<String>defer(snapshot -> {
            throw fatal;
        }).onErrorReturn("recovered").block()));
        assertNull(TLogContext.getTraceId());
    }

    @Test
    void unrelatedStreamsRetainSynchronousQueueFusion() {
        Flux<Integer> stream = Flux.range(1, 4).map(value -> value * 2);
        assertTrue(stream instanceof Fuseable);
        assertNotNull(Scannable.from(stream).scan(Scannable.Attr.PARENT));
        AtomicInteger total = new AtomicInteger();
        stream.subscribe(new CoreSubscriber<Integer>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSubscribe(Subscription subscription) {
                assertTrue(subscription instanceof Fuseable.QueueSubscription);
                Fuseable.QueueSubscription<Integer> queue = (Fuseable.QueueSubscription<Integer>) subscription;
                assertEquals(Fuseable.SYNC, queue.requestFusion(Fuseable.ANY));
                Integer next;
                while ((next = queue.poll()) != null) {
                    total.addAndGet(next);
                }
            }

            @Override
            public void onNext(Integer value) { fail("同步融合应使用队列读取"); }

            @Override
            public void onError(Throwable error) { fail(error); }

            @Override
            public void onComplete() { }
        });
        assertEquals(20, total.get());
        assertNull(TLogContext.getTraceId());
    }
}
