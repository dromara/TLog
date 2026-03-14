package com.yomahub.tlog.compat;

import java.lang.reflect.Method;

/**
 * Servlet API 反射工具类，用于兼容 javax.servlet 和 jakarta.servlet
 *
 * @author Claude
 * @since 1.6.0
 */
public class ServletReflectionUtils {

    private static final Method GET_HEADER_METHOD;
    private static final Method ADD_HEADER_METHOD;
    private static final Method GET_ATTRIBUTE_METHOD;
    private static final Method SET_ATTRIBUTE_METHOD;

    static {
        try {
            String packagePrefix = CompatibilityDetector.isJakarta() ? "jakarta.servlet" : "javax.servlet";
            Class<?> requestClass = Class.forName(packagePrefix + ".http.HttpServletRequest");
            Class<?> responseClass = Class.forName(packagePrefix + ".http.HttpServletResponse");

            GET_HEADER_METHOD = requestClass.getMethod("getHeader", String.class);
            ADD_HEADER_METHOD = responseClass.getMethod("addHeader", String.class, String.class);
            GET_ATTRIBUTE_METHOD = requestClass.getMethod("getAttribute", String.class);
            SET_ATTRIBUTE_METHOD = requestClass.getMethod("setAttribute", String.class, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ServletReflectionUtils", e);
        }
    }

    public static String getHeader(Object request, String name) {
        try {
            return (String) GET_HEADER_METHOD.invoke(request, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addHeader(Object response, String name, String value) {
        try {
            ADD_HEADER_METHOD.invoke(response, name, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getAttribute(Object request, String name) {
        try {
            return GET_ATTRIBUTE_METHOD.invoke(request, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setAttribute(Object request, String name, Object value) {
        try {
            SET_ATTRIBUTE_METHOD.invoke(request, name, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
