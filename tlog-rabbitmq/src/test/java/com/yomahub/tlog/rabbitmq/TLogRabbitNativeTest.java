package com.yomahub.tlog.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.LongString;
import com.rabbitmq.client.impl.LongStringHelper;
import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TLogRabbitNativeTest {

    @AfterEach
    void clean() {
        new TLogRPCHandler().cleanThreadLocal();
        TLogRuntime.setEnabled(true);
        TLogRuntime.setAppName(null);
    }

    @Test
    void injectReturnsNewPropertiesAndKeepsUserHeaders() {
        TLogRuntime.setAppName("rabbit-producer");
        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        AMQP.BasicProperties original = new AMQP.BasicProperties.Builder()
                .correlationId("c1").headers(Collections.singletonMap("user", "kept")).build();
        AMQP.BasicProperties props = TLogRabbitHeaders.inject(original);
        assertNotSame(original, props);
        assertEquals(1, original.getHeaders().size());
        assertEquals("c1", props.getCorrelationId());
        assertEquals("kept", props.getHeaders().get("user"));
        assertEquals("t1", props.getHeaders().get(TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.1", props.getHeaders().get(TLogConstants.TLOG_SPANID_KEY));
        assertEquals("rabbit-producer", props.getHeaders().get(TLogConstants.PRE_IVK_APP_KEY));
    }

    @Test
    void injectWithoutTraceOrWhenDisabledReturnsPropsUnchanged() {
        AMQP.BasicProperties original = new AMQP.BasicProperties();
        assertSame(original, TLogRabbitHeaders.inject(original));
        assertNull(TLogRabbitHeaders.inject((AMQP.BasicProperties) null));
        TLogContext.putTraceId("t1");
        TLogRuntime.setEnabled(false);
        assertSame(original, TLogRabbitHeaders.inject(original));
    }

    @Test
    void readsLongStringBytesAndStrings() {
        Map<String, Object> headers = new HashMap<>();
        LongString longString = LongStringHelper.asLongString("中文-trace");
        headers.put("a", longString);
        headers.put("b", "plain".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        headers.put("c", "string");
        assertEquals("中文-trace", TLogRabbitHeaders.read(headers, "a"));
        assertEquals("plain", TLogRabbitHeaders.read(headers, "b"));
        assertEquals("string", TLogRabbitHeaders.read(headers, "c"));
        assertNull(TLogRabbitHeaders.read(headers, "missing"));
        assertNull(TLogRabbitHeaders.read(null, "a"));
    }

    @Test
    void wrappedCallbackBuildsContextFromLongStringHeadersAndCleansUpOnFailure() {
        // 原生客户端收到的 header 是 LongString
        Map<String, Object> headers = new HashMap<>();
        headers.put(TLogConstants.TLOG_TRACE_KEY, LongStringHelper.asLongString("t1"));
        headers.put(TLogConstants.TLOG_SPANID_KEY, LongStringHelper.asLongString("0.1"));
        headers.put(TLogConstants.PRE_IVK_APP_KEY, LongStringHelper.asLongString("rabbit-producer"));
        Delivery delivery = delivery(headers);
        AtomicReference<String> seen = new AtomicReference<>();
        DeliverCallback callback = TLogRabbitConsumerProcessor.wrap((tag, d) -> {
            seen.set(TLogContext.getTraceId() + "|" + TLogContext.getSpanId() + "|" + MDC.get(TLogConstants.TLOG_TRACE_KEY)
                    + "|" + TLogContext.getPreIvkApp());
            throw new IllegalStateException("boom");
        });
        assertThrows(IllegalStateException.class, () -> callback.handle("tag", delivery));
        assertEquals("t1|0.1|t1|rabbit-producer", seen.get());
        assertNull(TLogContext.getTraceId());
        assertNull(MDC.get(TLogConstants.MDC_KEY));
    }

    @Test
    void processorGeneratesTraceWithoutHeadersAndHonoursSwitch() {
        AtomicReference<String> seen = new AtomicReference<>();
        TLogRabbitConsumerProcessor.process(delivery(null), d -> seen.set(TLogContext.getTraceId()));
        assertNotNull(seen.get());
        assertNull(TLogContext.getTraceId());

        TLogRuntime.setEnabled(false);
        seen.set("unset");
        TLogRabbitConsumerProcessor.process(delivery(Collections.singletonMap(TLogConstants.TLOG_TRACE_KEY, "t1")),
                d -> seen.set(TLogContext.getTraceId()));
        assertNull(seen.get());
    }

    private static Delivery delivery(Map<String, Object> headers) {
        return new Delivery(new Envelope(1, false, "", "q"),
                new AMQP.BasicProperties.Builder().headers(headers).build(), new byte[0]);
    }
}
