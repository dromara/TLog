package com.yomahub.tlog.core.enhance.log4j2.async.impl;

import com.yomahub.tlog.core.enhance.log4j2.async.AsyncTLogQueueFullPolicy;
import com.yomahub.tlog.core.enhance.log4j2.async.EventTLogRoute;
import org.apache.logging.log4j.Level;

/**
 * <pre>
 *    DefaultAsyncTLogQueueFullPolicy
 * </pre>
 * @author iwinkfc@dromara.org
 * @since 1.2.5
 * @see org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory
 */
public class DefaultAsyncTLogQueueFullPolicy implements AsyncTLogQueueFullPolicy {

    @Override
    public EventTLogRoute getRoute(final long backgroundThreadId, final Level level) {

        // LOG4J2-471: prevent deadlock when RingBuffer is full and object
        // being logged calls Logger.log() from its toString() method
        if (Thread.currentThread().getId() == backgroundThreadId) {
            return EventTLogRoute.SYNCHRONOUS;
        }
        return EventTLogRoute.ENQUEUE;
    }
}
