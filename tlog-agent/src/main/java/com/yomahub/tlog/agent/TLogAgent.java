package com.yomahub.tlog.agent;

import com.yomahub.tlog.core.enhance.bytes.AspectLogEnhance;

import java.lang.instrument.Instrumentation;

public class TLogAgent {

    public static void premain(String agentOps, Instrumentation inst) {
        System.out.println("[TLOG AGENT] START!");
        AspectLogEnhance.enhance();
    }
}
