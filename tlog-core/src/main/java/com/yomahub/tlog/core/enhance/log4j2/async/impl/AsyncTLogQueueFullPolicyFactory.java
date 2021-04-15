package com.yomahub.tlog.core.enhance.log4j2.async.impl;

import com.yomahub.tlog.core.enhance.log4j2.async.AsyncTLogQueueFullPolicy;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * <pre>
 *  AsyncTLogQueueFullPolicyFactory
 * </pre>
 * @author iwinkfc@dromara.org
 * @since 1.2.5
 * @see org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory
 */
public class AsyncTLogQueueFullPolicyFactory extends AsyncQueueFullPolicyFactory {
    static final String PROPERTY_NAME_ASYNC_EVENT_ROUTER = "log4j2.AsyncQueueFullPolicy";
    static final String PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER = "Default";

    public static AsyncTLogQueueFullPolicy createTLogQueuePolicy() {
        final String router = PropertiesUtil.getProperties().getStringProperty(PROPERTY_NAME_ASYNC_EVENT_ROUTER);
        if (router == null || isRouterSelected(
                router, DefaultAsyncTLogQueueFullPolicy.class, PROPERTY_VALUE_DEFAULT_ASYNC_EVENT_ROUTER)) {
            return new DefaultAsyncTLogQueueFullPolicy();
        }
        return createCustomRouter(router);
    }

    private static boolean isRouterSelected(
            String propertyValue,
            Class<? extends AsyncTLogQueueFullPolicy> policy,
            String shortPropertyValue) {
        return propertyValue != null && (shortPropertyValue.equalsIgnoreCase(propertyValue)
                || policy.getName().equals(propertyValue)
                || policy.getSimpleName().equals(propertyValue));
    }

    private static AsyncTLogQueueFullPolicy createCustomRouter(final String router) {
        try {
            final Class<? extends AsyncTLogQueueFullPolicy> cls = Loader.loadClass(router).asSubclass(AsyncTLogQueueFullPolicy.class);
            return cls.newInstance();
        } catch (final Exception ex) {
            return new DefaultAsyncTLogQueueFullPolicy();
        }
    }
}
