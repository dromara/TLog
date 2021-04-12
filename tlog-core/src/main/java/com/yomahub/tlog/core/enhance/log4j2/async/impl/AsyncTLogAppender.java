package com.yomahub.tlog.core.enhance.log4j2.async.impl;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.tlog.context.TLogContext;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.core.enhance.log4j2.async.AsyncTLogQueueFullPolicy;
import com.yomahub.tlog.core.enhance.log4j2.async.EventTLogRoute;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.async.ArrayBlockingQueueFactory;
import org.apache.logging.log4j.core.async.AsyncQueueFullMessageUtil;
import org.apache.logging.log4j.core.async.BlockingQueueFactory;
import org.apache.logging.log4j.core.async.InternalAsyncUtil;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MementoMessage;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicLong;


/**
 * <pre>
 *  自定义异步Plugin：AsyncTLog
 * </pre>
 *
 * @author iwinkfc@dromara.org
 * @see org.apache.logging.log4j.core.appender.AbstractAppender
 * @since 1.2.5
 */
@Plugin(name = "AsyncTLog", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class AsyncTLogAppender extends AbstractAppender {

    private static final LogEvent SHUTDOWN_LOG_EVENT = new AbstractLogEvent() {
        private static final long serialVersionUID = -1761035149477086338L;
    };

    private static final AtomicLong THREAD_SEQUENCE = new AtomicLong(1);

    private final BlockingQueue<LogEvent> queue;
    private final int queueSize;
    private final boolean blocking;
    private final long shutdownTimeout;
    private final Configuration config;
    private final AppenderRef[] appenderRefs;
    private final String errorRef;
    private final boolean includeLocation;
    private AppenderControl errorAppender;
    private AsyncThread thread;
    private AsyncTLogQueueFullPolicy asyncQueueFullPolicy;
    private Field simpleMessage = null;
    private Field parameterMessage = null;
    private Field objectMessage = null;
    private Field mementoMessage = null;
    private MutableLogEvent mutableLogEvent = null;
    private StringBuilder mutableLogBuilder = null;

    public AsyncTLogAppender(final String name, final Filter filter, final AppenderRef[] appenderRefs,
                             final String errorRef, final int queueSize, final boolean blocking, final boolean ignoreExceptions,
                             final long shutdownTimeout, final Configuration config, final boolean includeLocation,
                             final BlockingQueueFactory<LogEvent> blockingQueueFactory, final Property[] properties) {
        super(name, filter, null, ignoreExceptions);
        this.queue = blockingQueueFactory.create(queueSize);
        this.queueSize = queueSize;
        this.blocking = blocking;
        this.shutdownTimeout = shutdownTimeout;
        this.config = config;
        this.appenderRefs = appenderRefs;
        this.errorRef = errorRef;
        this.includeLocation = includeLocation;
    }

    @Override
    public void start() {
        final Map<String, Appender> map = config.getAppenders();
        final List<AppenderControl> appenders = new ArrayList<>();
        for (final AppenderRef appenderRef : appenderRefs) {
            final Appender appender = map.get(appenderRef.getRef());
            if (appender != null) {
                appenders.add(new AppenderControl(appender, appenderRef.getLevel(), appenderRef.getFilter()));
            } else {
                LOGGER.error("No appender named {} was configured", appenderRef);
            }
        }
        if (errorRef != null) {
            final Appender appender = map.get(errorRef);
            if (appender != null) {
                errorAppender = new AppenderControl(appender, null, null);
            } else {
                LOGGER.error("Unable to set up error Appender. No appender named {} was configured", errorRef);
            }
        }
        if (appenders.size() > 0) {
            thread = new AsyncThread(appenders, queue);
            thread.setName("AsyncTLogAppender-" + getName());
        } else if (errorRef == null) {
            throw new ConfigurationException("No appenders are available for AsyncTLogAppender " + getName());
        }
        asyncQueueFullPolicy = AsyncTLogQueueFullPolicyFactory.createTLogQueuePolicy();

        System.setProperty("Log4j2AsyncLoggerContextSelector", "com.yomahub.tlog.core.enhance.log4j2.async.impl.AsyncTLogAppender");
        thread.start();
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        LOGGER.trace("AsyncTLogAppender stopping. Queue still has {} events.", queue.size());
        thread.shutdown();
        try {
            thread.join(shutdownTimeout);
        } catch (final InterruptedException ex) {
            LOGGER.warn("Interrupted while stopping AsyncTLogAppender {}", getName());
        }
        LOGGER.trace("AsyncTLogAppender stopped. Queue has {} events.", queue.size());
        /*
        if (DiscardingAsyncQueueFullPolicy.getDiscardCount(asyncQueueFullPolicy) > 0) {
            LOGGER.trace("AsyncTLogAppender: {} discarded {} events.", asyncQueueFullPolicy,
                    DiscardingAsyncQueueFullPolicy.getDiscardCount(asyncQueueFullPolicy));
        }*/
        setStopped();
        return true;
    }

    /**
     * Actual writing occurs here.
     *
     * @param logEvent The LogEvent.
     */
    @Override
    public void append(final LogEvent logEvent) {
        if (!isStarted()) {
            throw new IllegalStateException("AsyncTLogAppender " + getName() + " is not active");
        }
        final Log4jLogEvent memento = Log4jLogEvent.createMemento(logEvent, includeLocation);
        final Message message = logEvent.getMessage();
        InternalAsyncUtil.makeMessageImmutable(logEvent.getMessage());

        String resultLog = message.getFormattedMessage();
        if (!TLogContext.hasTLogMDC()
                && StringUtils.isNotBlank(AspectLogContext.getLogValue())) {
            resultLog = StrUtil.format("{} {}", AspectLogContext.getLogValue(), resultLog);
        }
        final boolean isObj = message instanceof ObjectMessage;
        final boolean isSimple = message instanceof SimpleMessage;
        final boolean isParam = message instanceof ParameterizedMessage;
        final boolean isMemento = message instanceof MementoMessage;
        try {
            if (objectMessage == null && isObj) {
                objectMessage = ReflectUtil.getField(message.getClass(), "objectString");
                objectMessage.setAccessible(true);
            }
            if (simpleMessage == null && isSimple) {
                simpleMessage = ReflectUtil.getField(message.getClass(), "message");
                simpleMessage.setAccessible(true);
            }
            if (parameterMessage == null && isParam) {
                parameterMessage = ReflectUtil.getField(message.getClass(), "formattedMessage");
                parameterMessage.setAccessible(true);
            }
            if (mementoMessage == null && isMemento) {
                mementoMessage = ReflectUtil.getField(message.getClass(), "formattedMessage");
           /*     if(mutableLogMessage==null){
                    mutableLogBuilder = new StringBuilder(resultLog);
                    mutableLogEvent = (MutableLogEvent) logEvent;
                    message.
                    mutableLogEvent.setMessage();
                    final Message message1 = mutableLogEvent.getMessage();
                    mutableLogEvent.set
                }*/
                mementoMessage.setAccessible(true);
            }
            if (objectMessage != null && isObj) {
                objectMessage.set(message, resultLog);
            }
            if (parameterMessage != null && isParam) {
                parameterMessage.set(message, resultLog);
            }
            if (simpleMessage != null && isSimple) {
                simpleMessage.set(message, resultLog);
            }
            if (mementoMessage != null && isMemento) {
                mementoMessage.set(message, resultLog);
            }
        } catch (Exception e) {
        }
        if (!transfer(memento)) {
            if (blocking) {
                if (AbstractLogger.getRecursionDepth() > 1) { // LOG4J2-1518, LOG4J2-2031
                    // If queue is full AND we are in a recursive call, call appender directly to prevent deadlock
                    AsyncQueueFullMessageUtil.logWarningToStatusLogger();
                    logMessageInCurrentThread(logEvent);
                } else {
                    // delegate to the event router (which may discard, enqueue and block, or log in current thread)
                    final EventTLogRoute route = asyncQueueFullPolicy.getRoute(thread.getId(), memento.getLevel());
                    route.logMessage(this, memento);
                }
            } else {
                error("AsyncTLogAppender " + getName() + " is unable to write primary appenders. queue is full");
                logToErrorAppenderIfNecessary(false, memento);
            }
        }
    }

    private boolean transfer(final LogEvent memento) {
        return queue instanceof TransferQueue
                ? ((TransferQueue<LogEvent>) queue).tryTransfer(memento)
                : queue.offer(memento);
    }

    /**
     * FOR INTERNAL USE ONLY.
     *
     * @param logEvent the event to log
     */
    public void logMessageInCurrentThread(final LogEvent logEvent) {
        logEvent.setEndOfBatch(queue.isEmpty());
        final boolean appendSuccessful = thread.callAppenders(logEvent);
        logToErrorAppenderIfNecessary(appendSuccessful, logEvent);
    }

    /**
     * FOR INTERNAL USE ONLY.
     *
     * @param logEvent the event to log
     */
    public void logMessageInBackgroundThread(final LogEvent logEvent) {
        try {
            // wait for free slots in the queue
            queue.put(logEvent);
        } catch (final InterruptedException e) {
            final boolean appendSuccessful = handleInterruptedException(logEvent);
            logToErrorAppenderIfNecessary(appendSuccessful, logEvent);
        }
    }

    // LOG4J2-1049: Some applications use Thread.interrupt() to send
    // messages between application threads. This does not necessarily
    // mean that the queue is full. To prevent dropping a log message,
    // quickly try to offer the event to the queue again.
    // (Yes, this means there is a possibility the same event is logged twice.)
    //
    // Finally, catching the InterruptedException means the
    // interrupted flag has been cleared on the current thread.
    // This may interfere with the application's expectation of
    // being interrupted, so when we are done, we set the interrupted
    // flag again.
    private boolean handleInterruptedException(final LogEvent memento) {
        final boolean appendSuccessful = queue.offer(memento);
        if (!appendSuccessful) {
            LOGGER.warn("Interrupted while waiting for a free slot in the AsyncTLogAppender LogEvent-queue {}",
                    getName());
        }
        // set the interrupted flag again.
        Thread.currentThread().interrupt();
        return appendSuccessful;
    }

    private void logToErrorAppenderIfNecessary(final boolean appendSuccessful, final LogEvent logEvent) {
        if (!appendSuccessful && errorAppender != null) {
            errorAppender.callAppender(logEvent);
        }
    }


    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder<B extends Builder<B>> extends AbstractFilterable.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<AsyncTLogAppender> {

        @PluginElement("AppenderRef")
        @Required(message = "No appender references provided to AsyncTLogAppender")
        private AppenderRef[] appenderRefs;

        @PluginBuilderAttribute
        @PluginAliases("error-ref")
        private String errorRef;

        @PluginBuilderAttribute
        private boolean blocking = true;

        @PluginBuilderAttribute
        private long shutdownTimeout = 0L;

        @PluginBuilderAttribute
        private int bufferSize = 1024;

        @PluginBuilderAttribute
        @Required(message = "No name provided for AsyncTLogAppender")
        private String name;

        @PluginBuilderAttribute
        private boolean includeLocation = false;

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        private boolean ignoreExceptions = true;

        @PluginElement(BlockingQueueFactory.ELEMENT_TYPE)
        private BlockingQueueFactory<LogEvent> blockingQueueFactory = new ArrayBlockingQueueFactory<>();

        public AsyncTLogAppender.Builder setAppenderRefs(final AppenderRef[] appenderRefs) {
            this.appenderRefs = appenderRefs;
            return this;
        }

        public AsyncTLogAppender.Builder setErrorRef(final String errorRef) {
            this.errorRef = errorRef;
            return this;
        }

        public AsyncTLogAppender.Builder setBlocking(final boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        public AsyncTLogAppender.Builder setShutdownTimeout(final long shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        public AsyncTLogAppender.Builder setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public AsyncTLogAppender.Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public AsyncTLogAppender.Builder setIncludeLocation(final boolean includeLocation) {
            this.includeLocation = includeLocation;
            return this;
        }

        public AsyncTLogAppender.Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public AsyncTLogAppender.Builder setIgnoreExceptions(final boolean ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return this;
        }

        public AsyncTLogAppender.Builder setBlockingQueueFactory(final BlockingQueueFactory<LogEvent> blockingQueueFactory) {
            this.blockingQueueFactory = blockingQueueFactory;
            return this;
        }

        @Override
        public AsyncTLogAppender build() {
            return new AsyncTLogAppender(name, getFilter(), appenderRefs, errorRef, bufferSize, blocking, ignoreExceptions,
                    shutdownTimeout, configuration, includeLocation, blockingQueueFactory, null);
        }
    }

    /**
     * Thread that calls the Appenders.
     */
    private class AsyncThread extends Log4jThread {

        private volatile boolean shutdown = false;
        private final List<AppenderControl> appenders;
        private final BlockingQueue<LogEvent> queue;

        public AsyncThread(final List<AppenderControl> appenders, final BlockingQueue<LogEvent> queue) {
            super("AsyncTLogAppender-" + THREAD_SEQUENCE.getAndIncrement());
            this.appenders = appenders;
            this.queue = queue;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!shutdown) {
                LogEvent event;
                try {
                    event = queue.take();
                    if (event == SHUTDOWN_LOG_EVENT) {
                        shutdown = true;
                        continue;
                    }
                } catch (final InterruptedException ex) {
                    break; // LOG4J2-830
                }
                event.setEndOfBatch(queue.isEmpty());
                final boolean success = callAppenders(event);
                if (!success && errorAppender != null) {
                    try {
                        errorAppender.callAppender(event);
                    } catch (final Exception ex) {
                        // Silently accept the error.
                    }
                }
            }
            // Process any remaining items in the queue.
            LOGGER.trace("AsyncTLogAppender.AsyncThread shutting down. Processing remaining {} queue events.",
                    queue.size());
            int count = 0;
            int ignored = 0;
            while (!queue.isEmpty()) {
                try {
                    final LogEvent event = queue.take();
                    if (event instanceof Log4jLogEvent) {
                        final Log4jLogEvent logEvent = (Log4jLogEvent) event;
                        logEvent.setEndOfBatch(queue.isEmpty());
                        callAppenders(logEvent);
                        count++;
                    } else {
                        ignored++;
                        LOGGER.trace("Ignoring event of class {}", event.getClass().getName());
                    }
                } catch (final InterruptedException ex) {
                    // May have been interrupted to shut down.
                    // Here we ignore interrupts and try to process all remaining events.
                }
            }
            LOGGER.trace("AsyncTLogAppender.AsyncThread stopped. Queue has {} events remaining. "
                    + "Processed {} and ignored {} events since shutdown started.", queue.size(), count, ignored);
        }

        /**
         * Calls {@link AppenderControl#callAppender(LogEvent) callAppender} on all registered {@code AppenderControl}
         * objects, and returns {@code true} if at least one appender call was successful, {@code false} otherwise. Any
         * exceptions are silently ignored.
         *
         * @param event the event to forward to the registered appenders
         * @return {@code true} if at least one appender call succeeded, {@code false} otherwise
         */
        boolean callAppenders(final LogEvent event) {
            boolean success = false;
            for (final AppenderControl control : appenders) {
                try {
                    control.callAppender(event);
                    success = true;
                } catch (final Exception ex) {
                    // If no appender is successful the error appender will get it.
                }
            }
            return success;
        }

        public void shutdown() {
            shutdown = true;
            if (queue.isEmpty()) {
                queue.offer(SHUTDOWN_LOG_EVENT);
            }
            if (getState() == State.TIMED_WAITING || getState() == State.WAITING) {
                this.interrupt(); // LOG4J2-1422: if underlying appender is stuck in wait/sleep/join/park call
            }
        }
    }

    /**
     * Returns the names of the appenders that this AsyncTLogAppender delegates to as an array of Strings.
     *
     * @return the names of the sink appenders
     */
    public String[] getAppenderRefStrings() {
        final String[] result = new String[appenderRefs.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = appenderRefs[i].getRef();
        }
        return result;
    }

    /**
     * Returns {@code true} if this AsyncTLogAppender will take a snapshot of the stack with every log event to determine
     * the class and method where the logging call was made.
     *
     * @return {@code true} if location is included with every event, {@code false} otherwise
     */
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    /**
     * Returns {@code true} if this AsyncTLogAppender will block when the queue is full, or {@code false} if events are
     * dropped when the queue is full.
     *
     * @return whether this AsyncTLogAppender will block or drop events when the queue is full.
     */
    public boolean isBlocking() {
        return blocking;
    }

    /**
     * Returns the name of the appender that any errors are logged to or {@code null}.
     *
     * @return the name of the appender that any errors are logged to or {@code null}
     */
    public String getErrorRef() {
        return errorRef;
    }

    public int getQueueCapacity() {
        return queueSize;
    }

    public int getQueueRemainingCapacity() {
        return queue.remainingCapacity();
    }

    /**
     * Returns the number of elements in the queue.
     *
     * @return the number of elements in the queue.
     * @since 2.11.1
     */
    public int getQueueSize() {
        return queue.size();
    }
}
