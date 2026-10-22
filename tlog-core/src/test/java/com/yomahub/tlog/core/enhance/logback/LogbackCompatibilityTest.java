package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.enhance.logback.async.AspectLogbackAsyncAppender;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** 可用 logback/slf4j 版本覆盖参数复用同一组真实输出断言。 */
class LogbackCompatibilityTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void encoderAndAsyncAppenderPrintTraceAndPreserveMessage(boolean async) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger("tlog-compatibility-" + async);
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AspectLogbackEncoder encoder = new AspectLogbackEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%n");
        encoder.start();
        OutputStreamAppender<ILoggingEvent> target = new OutputStreamAppender<>();
        target.setContext(context);
        target.setName("capture");
        target.setEncoder(encoder);
        target.setOutputStream(output);
        target.start();
        AspectLogbackAsyncAppender queue = new AspectLogbackAsyncAppender();
        queue.setContext(context);
        queue.addAppender(target);
        if (async) {
            queue.start();
            logger.addAppender(queue);
        } else {
            logger.addAppender(target);
        }
        TLogRPCHandler handler = new TLogRPCHandler();
        boolean hadMdc = TLogContext.hasTLogMDC();
        try {
            TLogContext.setHasTLogMDC(false);
            handler.processProviderSide(key -> "tlogTraceId".equals(key) ? "request" : null);
            logger.info("payload {}", 7);
            handler.cleanThreadLocal();
            logger.info("clean");
        } finally {
            handler.cleanThreadLocal();
            queue.stop();
            target.stop();
            encoder.stop();
            logger.detachAndStopAllAppenders();
            TLogContext.setHasTLogMDC(hadMdc);
        }
        String text = output.toString("UTF-8");
        assertTrue(text.contains("<0><request> payload 7"), text);
        assertTrue(text.contains("\nclean\n"), text);
        assertFalse(text.contains("<0><request> clean"), text);
    }
}
