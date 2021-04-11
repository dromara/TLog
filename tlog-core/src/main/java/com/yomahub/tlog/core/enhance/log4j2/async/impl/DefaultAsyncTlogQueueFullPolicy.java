package com.yomahub.tlog.core.enhance.log4j2.async.impl;

import com.yomahub.tlog.core.enhance.log4j2.async.AsyncTlogQueueFullPolicy;
import com.yomahub.tlog.core.enhance.log4j2.async.EventTlogRoute;
import org.apache.logging.log4j.Level;

/**
 * <pre>
 *    DefaultAsyncTlogQueueFullPolicy
 * </pre>
 * @author: iwinkfc@dromara.org
 * @since: 1.2.5
 * @see org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory
 */
public class DefaultAsyncTlogQueueFullPolicy implements AsyncTlogQueueFullPolicy {

    @Override
    public EventTlogRoute getRoute(final long backgroundThreadId, final Level level) {

        // LOG4J2-471: prevent deadlock when RingBuffer is full and object
        // being logged calls Logger.log() from its toString() method
        if (Thread.currentThread().getId() == backgroundThreadId) {
            return EventTlogRoute.SYNCHRONOUS;
        }
        return EventTlogRoute.ENQUEUE;
    }
}
