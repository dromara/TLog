package com.yomahub.tlog.core.enhance.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import com.yomahub.tlog.context.TLogContext;

/**
 * logback 的 MDC 异步透传开关，在 logback.xml 中声明即可：
 * <pre>
 * &lt;contextListener class="com.yomahub.tlog.core.enhance.logback.TLogLogbackTTLMdcListener"/&gt;
 * </pre>
 *
 * @author Bryan.Zhang
 * @since 1.3.7
 */
public class TLogLogbackTTLMdcListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private volatile boolean started;

    @Override
    public void start() {
        TLogContext.setHasTLogMDC(true);
        // 必须使用自身持有的 context：此时 slf4j 仍在初始化中，LoggerFactory.getILoggerFactory() 返回的是临时替身
        if (!TLogLogbackTTLMdcAdapter.install(getContext())) {
            addWarn("[TLOG]MDC 适配器安装失败，MDC 将无法透传到子线程");
        }
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isResetResistant() {
        return true;
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
