package com.yomahub.tlog.core.rpc;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class W3cTraceContextTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "00-4bf92f3577b34da6a3ce929d0e0e4736-0000000000000000-01",
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902bg-01",
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-zz",
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7_01",
            "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01-extra",
            "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01extra",
            "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01-",
            "00-4BF92F3577B34DA6A3CE929D0E0E4736-00f067aa0ba902b7-01"
    })
    void rejectsMalformedHeaders(String value) {
        assertNull(W3cTraceContext.parseTraceId(value));
    }

    @Test
    void acceptsKnownFormatAndForwardCompatibleVersion() {
        String traceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        assertEquals(traceId, W3cTraceContext.parseTraceId("00-" + traceId + "-00f067aa0ba902b7-01"));
        assertEquals(traceId, W3cTraceContext.parseTraceId("01-" + traceId + "-00f067aa0ba902b7-01-extra"));
    }
}
