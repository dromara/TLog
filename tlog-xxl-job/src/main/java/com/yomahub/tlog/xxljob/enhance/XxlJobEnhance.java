package com.yomahub.tlog.xxljob.enhance;

import com.yomahub.tlog.context.TLogContext;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

/**
 *
 * @author zs
 * @since 1.3.0
 */
public class XxlJobEnhance {
    public static void enhance() {
        //xxl-job增强
        CtClass cc = null;
        ClassPool pool = ClassPool.getDefault();
        try {
            pool.importPackage("com.yomahub.tlog.xxljob.enhance.XxlJobBytesEnhance");
            cc = pool.get("com.xxl.job.core.server.EmbedServer$EmbedHttpServerHandler");
            if (cc != null) {
                CtMethod ctMethod = cc.getDeclaredMethod("channelRead0");
                ctMethod.insertBefore("XxlJobBytesEnhance.enhance($2);");
                cc.toClass();
                System.out.println("xxl-job同步增强成功");
                return;
            }
        } catch (Exception e) {
            System.out.println("xxl-job同步增强失败");
            e.printStackTrace();
        }
    }
}
