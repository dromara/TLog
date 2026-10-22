package com.yomahub.tlog.web.common;

import com.yomahub.tlog.core.context.TLogContextSnapshot;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Propagates MVC Callable context even when the framework's executor is not a Spring bean.
 * In particular, Spring Boot 2.0 uses an internal SimpleAsyncTaskExecutor for MVC.
 */
public final class TLogWebAsyncSupport {

    private static final String INTERCEPTOR_KEY = TLogWebAsyncSupport.class.getName();
    private static final Method GET_ASYNC_MANAGER = findAsyncManagerMethod();

    private TLogWebAsyncSupport() {
    }

    public static void register(Object servletRequest) {
        if (GET_ASYNC_MANAGER == null) {
            return;
        }
        WebAsyncManager manager = (WebAsyncManager) ReflectionUtils.invokeMethod(GET_ASYNC_MANAGER, null, servletRequest);
        if (manager.getCallableInterceptor(INTERCEPTOR_KEY) == null) {
            manager.registerCallableInterceptor(INTERCEPTOR_KEY, new ContextInterceptor());
        }
    }

    private static Method findAsyncManagerMethod() {
        if (!ClassUtils.isPresent("org.springframework.web.context.request.async.WebAsyncUtils",
                TLogWebAsyncSupport.class.getClassLoader())) {
            return null;
        }
        // The ServletRequest parameter changes from javax to jakarta in Spring 6.
        for (Method method : WebAsyncUtils.class.getMethods()) {
            if (method.getName().equals("getAsyncManager") && method.getParameterTypes().length == 1) {
                String parameter = method.getParameterTypes()[0].getName();
                if (parameter.equals("javax.servlet.ServletRequest") || parameter.equals("jakarta.servlet.ServletRequest")) {
                    return method;
                }
            }
        }
        throw new IllegalStateException("Unsupported WebAsyncUtils ServletRequest API");
    }

    private static final class ContextInterceptor implements CallableProcessingInterceptor {
        private volatile TLogContextSnapshot captured;
        private final ThreadLocal<TLogContextSnapshot.Scope> scopes = new ThreadLocal<>();

        @Override
        public <T> void beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) {
            captured = TLogContextSnapshot.capture();
        }

        @Override
        public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
            TLogContextSnapshot snapshot = captured;
            if (snapshot != null) {
                scopes.set(snapshot.restore());
            }
        }

        @Override
        public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
            TLogContextSnapshot.Scope scope = scopes.get();
            scopes.remove();
            if (scope != null) {
                scope.close();
            }
        }
    }
}
