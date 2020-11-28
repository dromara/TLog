package com.yomahub.tlog.core.mq;

/**
 * mq的具体业务执行接口
 *
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public interface TLogMqRunner<T> {

    void mqConsume(T t);

}
