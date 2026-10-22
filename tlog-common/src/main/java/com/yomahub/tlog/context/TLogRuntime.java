package com.yomahub.tlog.context;

import com.yomahub.tlog.constant.TLogConstants;

/**
 * Spring 无关的运行配置。纯 Java 应用也可直接设置应用名和总开关。
 */
public final class TLogRuntime {

    private static volatile boolean enabled = true;
    private static volatile String appName = TLogConstants.UNKNOWN;

    private TLogRuntime() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        TLogRuntime.enabled = enabled;
    }

    public static String getAppName() {
        return appName;
    }

    public static void setAppName(String appName) {
        TLogRuntime.appName = appName == null || appName.trim().isEmpty()
                ? TLogConstants.UNKNOWN : appName;
    }
}
