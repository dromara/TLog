package com.yomahub.tlog.xxljob.enhance;

import com.yomahub.tlog.spring.TLogSpringAware;
import com.yomahub.tlog.xxljob.common.TLogXxlJobCommon;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 *
 * @author zs
 * @since 1.3.0
 */
public class XxlJobBytesEnhance {
    public static void enhance(FullHttpRequest msg){
        TLogXxlJobCommon.loadInstance().handle(msg,TLogSpringAware.getProperty("spring.application.name"));
    }
}
