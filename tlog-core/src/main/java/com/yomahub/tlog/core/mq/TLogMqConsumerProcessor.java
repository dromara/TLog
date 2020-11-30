package com.yomahub.tlog.core.mq;

import com.yomahub.tlog.core.rpc.TLogLabelBean;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;

/**
 * tlog的mq消费者处理器
 * 
 * @author Bryan.Zhang
 * @since 1.2.0
 */
public class TLogMqConsumerProcessor {

    private static TLogRPCHandler tLogRPCHandler = new TLogRPCHandler();

    public static void process(TLogMqWrapBean tLogMqWrapBean, TLogMqRunner tLogMqRunner){
        tLogRPCHandler.processProviderSide(tLogMqWrapBean);
        try{
            tLogMqRunner.mqConsume(tLogMqWrapBean.getT());
        }finally {
            tLogRPCHandler.cleanThreadLocal();
        }
    }
}
