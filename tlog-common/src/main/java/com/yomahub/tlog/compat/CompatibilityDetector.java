package com.yomahub.tlog.compat;

/**
 * 运行时兼容性检测器，用于检测当前环境使用的是 javax 还是 jakarta 命名空间
 *
 * @author Claude
 * @since 1.6.0
 */
public class CompatibilityDetector {
    private static final boolean IS_JAKARTA;

    static {
        IS_JAKARTA = detectJakarta();
    }

    private static boolean detectJakarta() {
        try {
            Class.forName("jakarta.servlet.http.HttpServletRequest");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isJakarta() {
        return IS_JAKARTA;
    }
}
