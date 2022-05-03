package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import com.yomahub.tlog.context.TLogContext;
import org.slf4j.TLogLogbackTTLMdcAdapter;

public class TLogLogbackTTLMdcListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {
    @Override
    public void start() {
        TLogContext.setHasTLogMDC(true);
        TLogLogbackTTLMdcAdapter.getInstance();
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isResetResistant() {
        return false;
    }

    @Override
    public void onStart(LoggerContext loggerContext) {

    }

    @Override
    public void onReset(LoggerContext loggerContext) {

    }

    @Override
    public void onStop(LoggerContext loggerContext) {

    }

    @Override
    public void onLevelChange(Logger logger, Level level) {

    }
}
