package org.slf4j;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.slf4j.spi.MDCAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 针对于Logback日志的mdc adapter。
 *
 * @author Bryan.Zhang
 * @since 1.3.7
 */
public class TLogLogbackTTLMdcAdapter implements MDCAdapter {

    final ThreadLocal<Map<String, String>> copyOnInheritThreadLocal = new TransmittableThreadLocal<>();

    private static final int WRITE_OPERATION = 1;
    private static final int READ_OPERATION = 2;

    private static TLogLogbackTTLMdcAdapter mdcMDCAdapter;

    // keeps track of the last operation performed
    final ThreadLocal<Integer> lastOperation = new ThreadLocal<>();

    static {
        mdcMDCAdapter = new TLogLogbackTTLMdcAdapter();
        MDC.mdcAdapter = mdcMDCAdapter;
    }

    public static MDCAdapter getInstance() {
        return mdcMDCAdapter;
    }

    private Integer getAndSetLastOperation(int op) {
        Integer lastOp = lastOperation.get();
        lastOperation.set(op);
        return lastOp;
    }

    private static boolean wasLastOpReadOrNull(Integer lastOp) {
        return lastOp == null || lastOp == READ_OPERATION;
    }

    private Map<String, String> duplicateAndInsertNewMap(Map<String, String> oldMap) {
        Map<String, String> newMap = Collections.synchronizedMap(new HashMap<String, String>());
        if (oldMap != null) {
            // we don't want the parent thread modifying oldMap while we are
            // iterating over it
            synchronized (oldMap) {
                newMap.putAll(oldMap);
            }
        }

        copyOnInheritThreadLocal.set(newMap);
        return newMap;
    }

    @Override
    public void put(String key, String val) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        Map<String, String> oldMap = copyOnInheritThreadLocal.get();
        Integer lastOp = getAndSetLastOperation(WRITE_OPERATION);

        if (wasLastOpReadOrNull(lastOp) || oldMap == null) {
            Map<String, String> newMap = duplicateAndInsertNewMap(oldMap);
            newMap.put(key, val);
        } else {
            oldMap.put(key, val);
        }
    }

    @Override
    public void remove(String key) {
        if (key == null) {
            return;
        }
        Map<String, String> oldMap = copyOnInheritThreadLocal.get();
        if (oldMap == null) {
            return;
        }

        Integer lastOp = getAndSetLastOperation(WRITE_OPERATION);

        if (wasLastOpReadOrNull(lastOp)) {
            Map<String, String> newMap = duplicateAndInsertNewMap(oldMap);
            newMap.remove(key);
        } else {
            oldMap.remove(key);
        }

    }


    @Override
    public void clear() {
        lastOperation.set(WRITE_OPERATION);
        copyOnInheritThreadLocal.remove();
    }

    @Override
    public String get(String key) {
        Map<String, String> map = getPropertyMap();
        if ((map != null) && (key != null)) {
            return map.get(key);
        } else {
            return null;
        }
    }

    public Map<String, String> getPropertyMap() {
        lastOperation.set(READ_OPERATION);
        return copyOnInheritThreadLocal.get();
    }


    @Override
    public Map getCopyOfContextMap() {
        lastOperation.set(READ_OPERATION);
        Map<String, String> hashMap = copyOnInheritThreadLocal.get();
        if (hashMap == null) {
            return null;
        } else {
            return new HashMap<>(hashMap);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setContextMap(Map contextMap) {
        lastOperation.set(WRITE_OPERATION);

        Map<String, String> newMap = Collections.synchronizedMap(new HashMap<>());
        newMap.putAll(contextMap);

        // the newMap replaces the old one for serialisation's sake
        copyOnInheritThreadLocal.set(newMap);


    }
}
