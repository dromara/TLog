package com.yomahub.tlog;

import com.yomahub.tlog.exception.TLogCustomLabelExpressionException;
import com.yomahub.tlog.utils.AbstractClock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyApiTest {
    @Test
    void legacyExceptionSetterPreservesCauseAndClockApiRemainsCallable() {
        Throwable cause = new IllegalArgumentException("cause");
        TLogCustomLabelExpressionException exception = new TLogCustomLabelExpressionException("old", cause);
        exception.setMessage("new");
        assertEquals("new", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertTrue(AbstractClock.systemClock().millis() > 0);
        AbstractClock fixed = new AbstractClock() {
            @Override
            public long millis() {
                return 42;
            }
        };
        assertEquals(42, fixed.millis());
    }
}
