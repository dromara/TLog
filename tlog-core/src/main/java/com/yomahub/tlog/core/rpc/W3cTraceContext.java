package com.yomahub.tlog.core.rpc;

/**
 * W3C Trace Context 的 {@code traceparent} 解析
 * <p>
 * 格式：{@code version-traceid-parentid-flags}，例如 {@code 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01}。
 * 上游（OpenTelemetry、SkyWalking 网关等）未携带 TLog 头时，沿用其中的 traceId，使两套链路可以用同一个 ID 检索。
 *
 * @author Bryan.Zhang
 * @since 2.0.0
 */
public final class W3cTraceContext {

    private static final int TRACE_ID_LENGTH = 32;

    private static final String INVALID_TRACE_ID = "00000000000000000000000000000000";

    private W3cTraceContext() {
    }

    /**
     * @return 合法时返回 32 位小写十六进制 traceId，否则返回 null
     */
    public static String parseTraceId(String traceParent) {
        if (traceParent == null) {
            return null;
        }
        String value = traceParent.trim();
        // 版本号 2 位 + '-' + traceId 32 位 + '-' + parentId 16 位，未来版本可能在末尾追加字段
        if (value.length() < 55 || value.charAt(2) != '-' || value.charAt(35) != '-'
                || value.charAt(52) != '-') {
            return null;
        }
        String version = value.substring(0, 2);
        if (!isLowerHex(version) || "ff".equals(version)) {
            return null;
        }
        if ("00".equals(version) && value.length() != 55) {
            return null;
        }
        if (value.length() > 55 && (value.charAt(55) != '-' || value.length() == 56)) {
            return null;
        }
        String traceId = value.substring(3, 3 + TRACE_ID_LENGTH);
        if (!isLowerHex(traceId) || INVALID_TRACE_ID.equals(traceId)) {
            return null;
        }
        String parentId = value.substring(36, 52);
        if (!isLowerHex(parentId) || "0000000000000000".equals(parentId)
                || !isLowerHex(value.substring(53, 55))) {
            return null;
        }
        return traceId;
    }

    private static boolean isLowerHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }
}
