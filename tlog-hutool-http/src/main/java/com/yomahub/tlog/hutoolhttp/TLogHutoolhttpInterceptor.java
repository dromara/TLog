package com.yomahub.tlog.hutoolhttp;

import cn.hutool.http.HttpBase;
import cn.hutool.http.HttpInterceptor;
import com.yomahub.tlog.context.TLogPropagator;

/**
 * Hutool http组件的拦截器
 *
 * @author Bryan.Zhang
 * @since 1.3.5
 */
@SuppressWarnings("rawtypes")
public class TLogHutoolhttpInterceptor implements HttpInterceptor {

    @Override
    public void process(HttpBase httpBase) {
        TLogPropagator.inject((name, value) -> httpBase.header(name, value, true));
    }
}
