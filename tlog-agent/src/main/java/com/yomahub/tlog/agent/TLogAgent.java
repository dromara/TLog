package com.yomahub.tlog.agent;

import com.yomahub.tlog.core.enhance.bytes.AspectLogEnhance;

import java.lang.instrument.Instrumentation;

/**
 * TLog的javaAgent类<br>
 * 内部也是通过字节码的方式进行增强，只不过把字节码增强的阶段提早到类加载器之前
 *
 * @author Bryan.Zhang
 * @since 1.1.2
 */
public class TLogAgent {

    public static void premain(String agentOps, Instrumentation inst) {
        System.out.println("[TLOG AGENT] START!");
        AspectLogEnhance.enhance();
    }
}
