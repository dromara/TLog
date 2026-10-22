package com.yomahub.tlog.utils;

/** 保留 1.x 的时钟扩展 API。 */
public abstract class AbstractClock {

    public static AbstractClock systemClock() {
        return new AbstractClock() {
            @Override
            public long millis() {
                return System.currentTimeMillis();
            }
        };
    }

    public abstract long millis();
}
