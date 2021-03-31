package com.yomahub.tlog.core.enhance.log4j2.async;

import org.apache.logging.log4j.Level;

/**
 * <pre>
 *  自定义异步队列操作策略
 * </pre>
 * @author: iwinkfc@dromara.org
 * @since: 1.2.5
 * @see org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory
 */
public interface AsyncTlogQueueFullPolicy {

    /**
     * Returns the appropriate route for the current log event, given the specified parameters.
     *
     * @param backgroundThreadId the thread ID of the background thread. Can be compared with the current thread's ID.
     * @param level              the level of the log event
     * @return the appropriate route for the current event
     */
    EventTlogRoute getRoute(final long backgroundThreadId, final Level level);
}
