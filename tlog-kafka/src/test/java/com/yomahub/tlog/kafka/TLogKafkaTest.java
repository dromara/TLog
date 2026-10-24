package com.yomahub.tlog.kafka;

import com.yomahub.tlog.constant.TLogConstants;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.context.TLogRuntime;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TLogKafkaTest {

    @AfterEach
    void clean() {
        new TLogRPCHandler().cleanThreadLocal();
        TLogRuntime.setEnabled(true);
        TLogRuntime.setAppName(null);
    }

    @Test
    void producerInterceptorWritesHeaders() {
        TLogRuntime.setAppName("kafka-producer");
        TLogContext.putTraceId("t1");
        TLogContext.putSpanId("0");
        ProducerRecord<String, String> record = new TLogKafkaProducerInterceptor<String, String>()
                .onSend(new ProducerRecord<>("topic", "value"));
        assertEquals("t1", TLogKafkaHeaders.read(record.headers(), TLogConstants.TLOG_TRACE_KEY));
        assertEquals("0.1", TLogKafkaHeaders.read(record.headers(), TLogConstants.TLOG_SPANID_KEY));
        assertEquals("kafka-producer", TLogKafkaHeaders.read(record.headers(), TLogConstants.PRE_IVK_APP_KEY));
    }

    @Test
    void producerReplacesStaleHeaderInsteadOfAppending() {
        TLogContext.putTraceId("new");
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "value");
        record.headers().add(TLogConstants.TLOG_TRACE_KEY, "old".getBytes(StandardCharsets.UTF_8));
        new TLogKafkaProducerInterceptor<String, String>().onSend(record);
        int traceHeaders = 0;
        for (org.apache.kafka.common.header.Header ignored : record.headers().headers(TLogConstants.TLOG_TRACE_KEY)) {
            traceHeaders++;
        }
        assertEquals(1, traceHeaders);
        assertEquals("new", TLogKafkaHeaders.read(record.headers(), TLogConstants.TLOG_TRACE_KEY));
    }

    @Test
    void producerWithoutTraceOrReadOnlyHeadersLeavesRecordAlone() {
        ProducerRecord<String, String> record = new ProducerRecord<>("topic", "value");
        new TLogKafkaProducerInterceptor<String, String>().onSend(record);
        assertEquals(0, record.headers().toArray().length);

        TLogContext.putTraceId("t1");
        ((RecordHeaders) record.headers()).setReadOnly();
        assertSame(record, assertDoesNotThrow(() -> new TLogKafkaProducerInterceptor<String, String>().onSend(record)));
        assertEquals(0, record.headers().toArray().length);
    }

    @Test
    void producerRespectsMasterSwitch() {
        TLogRuntime.setEnabled(false);
        TLogContext.putTraceId("t1");
        ProducerRecord<String, String> record = new TLogKafkaProducerInterceptor<String, String>()
                .onSend(new ProducerRecord<>("topic", "value"));
        assertEquals(0, record.headers().toArray().length);
    }

    @Test
    void consumerProcessorBuildsContextAndCleansUpOnFailure() {
        ConsumerRecord<String, String> record = record("t1", "0.1");
        AtomicReference<String> seen = new AtomicReference<>();
        assertThrows(IllegalStateException.class, () -> TLogKafkaConsumerProcessor.process(record, r -> {
            seen.set(TLogContext.getTraceId() + "|" + TLogContext.getSpanId() + "|" + MDC.get(TLogConstants.TLOG_TRACE_KEY)
                    + "|" + TLogContext.getPreIvkApp());
            throw new IllegalStateException("boom");
        }));
        assertEquals("t1|0.1|t1|kafka-producer", seen.get());
        assertNull(TLogContext.getTraceId());
        assertNull(MDC.get(TLogConstants.TLOG_TRACE_KEY));
    }

    @Test
    void consumerWithoutHeadersGeneratesTraceAndW3cIsHonoured() {
        AtomicReference<String> seen = new AtomicReference<>();
        TLogKafkaConsumerProcessor.process(new ConsumerRecord<>("topic", 0, 0, "k", "v"), r -> seen.set(TLogContext.getTraceId()));
        assertNotNull(seen.get());

        ConsumerRecord<String, String> w3c = new ConsumerRecord<>("topic", 0, 1, "k", "v");
        w3c.headers().add(TLogConstants.W3C_TRACE_PARENT,
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01".getBytes(StandardCharsets.UTF_8));
        TLogKafkaConsumerProcessor.process(w3c, r -> seen.set(TLogContext.getTraceId()));
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", seen.get());
    }

    @Test
    void consumerRespectsMasterSwitch() {
        TLogRuntime.setEnabled(false);
        AtomicReference<String> seen = new AtomicReference<>("unset");
        TLogKafkaConsumerProcessor.process(record("t1", "0.1"), r -> seen.set(TLogContext.getTraceId()));
        assertNull(seen.get());
    }

    static ConsumerRecord<String, String> record(String trace, String span) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0, "k", "v");
        record.headers().add(TLogConstants.TLOG_TRACE_KEY, trace.getBytes(StandardCharsets.UTF_8));
        record.headers().add(TLogConstants.TLOG_SPANID_KEY, span.getBytes(StandardCharsets.UTF_8));
        record.headers().add(TLogConstants.PRE_IVK_APP_KEY, "kafka-producer".getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
