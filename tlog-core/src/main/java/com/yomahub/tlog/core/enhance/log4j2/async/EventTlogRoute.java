package com.yomahub.tlog.core.enhance.log4j2.async;

import com.yomahub.tlog.core.enhance.log4j2.async.impl.AsyncTlogAppender;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.message.Message;

/**
 * <pre>
 * 自定义EventTlogRoute
 * </pre>
 * @author iwinkfc@dromara.org
 * @since 1.2.5
 * @see org.apache.logging.log4j.core.async.EventRoute
 */
public enum EventTlogRoute {

    /**
     * Enqueues the event for asynchronous logging in the background thread.
     */
    ENQUEUE {
        @Override
        public void logMessage(final AsyncLogger asyncLogger, final String fqcn, final Level level,
                               final Marker marker, final Message message, final Throwable thrown) {
        }

        @Override
        public void logMessage(final AsyncLoggerConfig asyncLoggerConfig, final LogEvent event) {
            // asyncLoggerConfig.logInBackgroundThread(event);
        }

        @Override
        public void logMessage(final AsyncAppender asyncAppender, final LogEvent logEvent) {
            asyncAppender.logMessageInBackgroundThread(logEvent);
        }

        @Override
        public void logMessage(AsyncTlogAppender asyncAppender, LogEvent coreEvent) {
            asyncAppender.logMessageInBackgroundThread(coreEvent);
        }
    },
    /**
     * Logs the event synchronously: sends the event directly to the appender (in the current thread).
     * WARNING: This may result in lines logged out of order as synchronous events may be persisted before
     * earlier events, even from the same thread, which wait in the queue.
     */
    SYNCHRONOUS {
        @Override
        public void logMessage(final AsyncLogger asyncLogger, final String fqcn, final Level level,
                               final Marker marker, final Message message, final Throwable thrown) {
        }

        @Override
        public void logMessage(final AsyncLoggerConfig asyncLoggerConfig, final LogEvent event) {
            //asyncLoggerConfig.logToAsyncLoggerConfigsOnCurrentThread(event);
        }

        @Override
        public void logMessage(final AsyncAppender asyncAppender, final LogEvent logEvent) {
            asyncAppender.logMessageInCurrentThread(logEvent);
        }

        @Override
        public void logMessage(AsyncTlogAppender asyncAppender, LogEvent coreEvent) {
            asyncAppender.logMessageInBackgroundThread(coreEvent);
        }
    },
    /**
     * Discards the event (so it is not logged at all).
     */
    DISCARD {
        @Override
        public void logMessage(final AsyncLogger asyncLogger, final String fqcn, final Level level,
                               final Marker marker, final Message message, final Throwable thrown) {
            // do nothing: drop the event
        }

        @Override
        public void logMessage(final AsyncLoggerConfig asyncLoggerConfig, final LogEvent event) {
            // do nothing: drop the event
        }

        @Override
        public void logMessage(final AsyncAppender asyncAppender, final LogEvent coreEvent) {
            // do nothing: drop the event
        }

        @Override
        public void logMessage(AsyncTlogAppender asyncAppender, LogEvent coreEvent) {
            // do nothing: drop the event
        }
    };

    public abstract void logMessage(final AsyncLogger asyncLogger, final String fqcn, final Level level,
                                    final Marker marker, final Message message, final Throwable thrown);

    public abstract void logMessage(final AsyncLoggerConfig asyncLoggerConfig, final LogEvent event);

    public abstract void logMessage(final AsyncAppender asyncAppender, final LogEvent coreEvent);

    public abstract void logMessage(final AsyncTlogAppender asyncAppender, final LogEvent coreEvent);
}
