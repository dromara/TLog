package com.yomahub.tlog.core.aop;

import com.yomahub.tlog.core.annotation.TLogAspect;
import com.yomahub.tlog.core.context.AspectLogContext;
import com.yomahub.tlog.core.convert.AspectLogConvert;
import com.yomahub.tlog.core.rpc.TLogRPCHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AspectLogAopTest {

    public interface NamedService {
        String invoke(String name);
    }

    public static class NamedServiceImpl implements NamedService {
        @Override
        @TLogAspect("name")
        public String invoke(String name) {
            return AspectLogContext.getLogValue();
        }
    }

    @Test
    void jdkProxyFindsAnnotationAndParameterNamesOnImplementation() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new NamedServiceImpl());
        factory.setProxyTargetClass(false);
        factory.addAspect(new AspectLogAop());
        NamedService service = factory.getProxy();
        assertTrue(java.lang.reflect.Proxy.isProxyClass(service.getClass()));
        assertEquals("[name:alice]", service.invoke("alice"));
        assertNull(AspectLogContext.getLogValue());
    }

    public static class User {
        private final String name;

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class NameConvert implements AspectLogConvert {
        @Override
        public String convert(Object[] args) {
            return "conv-" + ((User) args[0]).getName();
        }
    }

    public static class Service {
        final AtomicReference<String> seen = new AtomicReference<>();
        Service self;

        @TLogAspect({"user.name", "id"})
        public void expression(User user, Long id) {
            seen.set(AspectLogContext.getLogValue());
        }

        @TLogAspect(str = "outer")
        public void outer() {
            self.inner();
            seen.set(AspectLogContext.getLogValue());
        }

        @TLogAspect(str = "inner")
        public void inner() {
            seen.set(AspectLogContext.getLogValue());
        }

        @TLogAspect(convert = NameConvert.class)
        public void converted(User user) {
            seen.set(AspectLogContext.getLogValue());
        }

        @TLogAspect(str = "boom")
        public void fail() {
            throw new IllegalStateException("boom");
        }
    }

    private final TLogRPCHandler handler = new TLogRPCHandler();

    @AfterEach
    void clean() {
        handler.cleanThreadLocal();
    }

    /**
     * CGLIB 代理对象自身的字段不会被初始化，断言统一从目标对象读取
     */
    private final Service target = new Service();

    private Service proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new AspectLogAop());
        Service proxy = factory.getProxy();
        target.self = proxy;
        return proxy;
    }

    @Test
    void appendsExpressionLabelAndRestoresAfterwards() {
        handler.processProviderSide(name -> null);
        String base = AspectLogContext.getLogValue();
        Service service = proxy();

        service.expression(new User("jack"), 7L);

        assertEquals(base + " [user.name:jack,id:7]", target.seen.get());
        assertEquals(base, AspectLogContext.getLogValue(), "方法返回后应恢复原标签");
    }

    @Test
    void noTLogContextDoesNotProduceNullPrefix() {
        Service service = proxy();
        service.expression(new User("jack"), 7L);
        assertEquals("[user.name:jack,id:7]", target.seen.get());
        assertNull(AspectLogContext.getLogValue());
    }

    @Test
    void nestedCallsDoNotAccumulate() {
        handler.processProviderSide(name -> null);
        String base = AspectLogContext.getLogValue();
        Service service = proxy();

        service.outer();

        assertEquals(base + " [outer]", target.seen.get(), "inner 返回后应回到 outer 的标签");
        assertEquals(base, AspectLogContext.getLogValue());
    }

    @Test
    void customConvertIsApplied() {
        Service service = proxy();
        service.converted(new User("rose"));
        assertEquals("[conv-rose]", target.seen.get());
    }

    @Test
    void labelIsRestoredWhenMethodThrows() {
        handler.processProviderSide(name -> null);
        String base = AspectLogContext.getLogValue();
        Service service = proxy();
        assertThrows(IllegalStateException.class, service::fail);
        assertEquals(base, AspectLogContext.getLogValue());
    }
}
