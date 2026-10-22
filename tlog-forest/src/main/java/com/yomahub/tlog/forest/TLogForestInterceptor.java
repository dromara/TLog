package com.yomahub.tlog.forest;

import com.dtflys.forest.http.ForestRequest;
import com.dtflys.forest.interceptor.Interceptor;
import com.yomahub.tlog.context.TLogPropagator;

/**
 * Forest的拦截器
 *
 * @author Bryan.Zhang
 * @since 1.3.5
 */
public class TLogForestInterceptor implements Interceptor<Object> {

    @Override
    @SuppressWarnings("rawtypes")
    public boolean beforeExecute(ForestRequest request) {
        TLogPropagator.inject(request::addHeader);
        return true;
    }
}
