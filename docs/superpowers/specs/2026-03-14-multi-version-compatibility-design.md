# TLog 多版本兼容性升级设计文档

**版本**: 1.6.0
**日期**: 2026-03-14
**作者**: Claude
**状态**: 设计中

## 1. 概述

### 1.1 目标

将 TLog 从当前仅支持 Java 8 和 Spring Boot 2.X 升级到支持：
- **Java 版本**: Java 8 ~ Java 25
- **Spring Boot 版本**: Spring Boot 2.X ~ Spring Boot 4.X
- **项目版本**: 更新到 1.6.0

### 1.2 核心约束

- ✅ 单一构建产物（一个 JAR 支持所有版本）
- ✅ 完全向后兼容（无破坏性变更）
- ✅ 运行时检测适配（javax/jakarta 命名空间）
- ✅ 关键组合测试（Java 8+SB2, Java 17+SB3, Java 21+SB3, Java 25+SB4）

### 1.3 当前状态

```xml
<version>1.5.3</version>
<maven.compiler.source>8</maven.compiler.source>
<maven.compiler.target>8</maven.compiler.target>
<spring-boot.version>2.0.5.RELEASE</spring-boot.version>
<spring.version>5.0.9.RELEASE</spring.version>
```

## 2. 技术方案

### 2.1 方案选择

**采用方案：保守型 - 依赖可选化 + 运行时适配**

**理由**：
1. 实现成本低，风险可控
2. 完全满足单一产物和向后兼容要求
3. TLog 作为日志框架，运行时检测开销可忽略
4. 易于维护和测试

### 2.2 核心策略

#### 2.2.1 编译目标
- 保持 Java 8 字节码兼容性
- 使用 `--release 8` 确保 API 兼容性
- 不使用 Java 9+ 特有 API

#### 2.2.2 依赖管理
```xml
<!-- 所有 Spring Boot 相关依赖 -->
<optional>true</optional>
<scope>provided</scope>
```

**关键依赖版本策略**：
- Spring Framework: 5.0.9+ (兼容 SB 2.X-4.X)
- Spring Boot: 用户自行引入，TLog 不强制版本
- Servlet API: 运行时适配 javax.servlet / jakarta.servlet

#### 2.2.3 命名空间适配

创建运行时检测机制处理 javax → jakarta 迁移：

```java
// 伪代码示例
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
```

## 3. 实施计划

### 3.1 Phase 1: 依赖升级与可选化

**目标**: 更新依赖版本，标记为 optional/provided

**任务**：
1. 更新 `pom.xml` 版本号到 1.6.0
2. 升级核心依赖到兼容版本：
   - Spring Framework: 5.3.x (兼容 SB 2.X-3.X)
   - 日志框架保持现有版本（已兼容）
3. 将所有 Spring Boot 依赖标记为 `optional` + `provided`
4. 更新子模块 pom.xml 继承父版本

**影响范围**：
- `/pom.xml`
- `/tlog-spring-boot/*/pom.xml`
- 所有子模块 pom.xml

### 3.2 Phase 2: 创建兼容性适配层

**目标**: 实现运行时检测和适配

**任务**：
1. 创建 `CompatibilityDetector` 类
2. 创建 Servlet API 适配器（如需要）
3. 更新涉及 javax.servlet 的代码使用适配器

**新增文件**：
- `tlog-common/src/main/java/com/yomahub/tlog/compat/CompatibilityDetector.java`
- `tlog-common/src/main/java/com/yomahub/tlog/compat/ServletApiAdapter.java`（如需要）

**修改文件**：
- `tlog-webroot/src/main/java/com/yomahub/tlog/web/filter/TLogServletFilter.java`
- `tlog-webroot/src/main/java/com/yomahub/tlog/web/wrapper/RequestWrapper.java`
- 其他使用 Servlet API 的类

### 3.3 Phase 3: Spring Boot 自动配置适配

**目标**: 确保自动配置在所有 Spring Boot 版本下工作

**任务**：
1. 检查 `spring.factories` / `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
2. 确保条件注解兼容（`@ConditionalOnClass` 等）
3. 测试自动配置在不同版本下的加载

**影响范围**：
- `tlog-spring-boot/tlog-spring-boot-configuration/src/main/resources/META-INF/`
- 所有 `*AutoConfiguration.java` 类

### 3.4 Phase 4: 测试验证

**目标**: 验证关键组合的兼容性

**测试矩阵**：
| Java 版本 | Spring Boot 版本 | 测试重点 |
|-----------|------------------|----------|
| Java 8    | 2.7.x (最新 2.X) | 向后兼容性 |
| Java 17   | 3.2.x            | javax → jakarta |
| Java 21   | 3.3.x            | 新 Java 特性兼容 |
| Java 25   | 4.0.x (如可用)   | 最新版本兼容 |

**测试内容**：
1. 编译测试（所有 Java 版本）
2. 单元测试（核心功能）
3. 集成测试（tlog-example 项目）
4. 性能测试（确保无明显退化）

### 3.5 Phase 5: 文档更新

**任务**：
1. 更新 README.md 说明支持的版本
2. 创建升级指南（1.5.x → 1.6.0）
3. 更新依赖配置示例
4. 添加多版本使用说明

## 4. 技术细节

### 4.1 关键依赖版本选择

```xml
<properties>
    <!-- 保持 Java 8 编译 -->
    <maven.compiler.source>8</maven.compiler.source>
    <maven.compiler.target>8</maven.compiler.target>
    <maven.compiler.release>8</maven.compiler.release>

    <!-- Spring 版本：选择兼容范围最广的版本 -->
    <spring.version>5.3.31</spring.version>  <!-- 兼容 SB 2.X-3.X -->

    <!-- Spring Boot：不强制版本，由用户决定 -->
    <!-- <spring-boot.version> 移除或标记为示例 -->

    <!-- 其他依赖保持现有版本 -->
</properties>
```

### 4.2 Servlet API 处理

**问题**: Spring Boot 3.X+ 使用 jakarta.servlet，2.X 使用 javax.servlet

**解决方案**：

**选项 A（推荐）**: 使用反射和接口抽象
```java
public interface ServletRequestAdapter {
    String getHeader(String name);
    void setAttribute(String name, Object value);
    // ...
}

// 运行时创建适配实例
public class ServletAdapterFactory {
    public static ServletRequestAdapter wrap(Object request) {
        if (CompatibilityDetector.isJakarta()) {
            return new JakartaServletRequestAdapter(request);
        } else {
            return new JavaxServletRequestAdapter(request);
        }
    }
}
```

**选项 B（更简单）**: 直接使用反射
```java
public class ServletUtils {
    public static String getHeader(Object request, String name) {
        try {
            Method method = request.getClass().getMethod("getHeader", String.class);
            return (String) method.invoke(request, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

**推荐**: 先尝试选项 B，如果代码复杂度高再考虑选项 A

### 4.3 Spring Boot 自动配置兼容性

**Spring Boot 2.X**: 使用 `META-INF/spring.factories`
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.yomahub.tlog.springboot.TLogCommonAutoConfiguration
```

**Spring Boot 3.X+**: 使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
```
com.yomahub.tlog.springboot.TLogCommonAutoConfiguration
```

**解决方案**: 同时提供两个文件，Spring Boot 会自动选择

### 4.4 Maven 编译器配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>8</source>
        <target>8</target>
        <release>8</release>  <!-- 确保 API 兼容性 -->
        <encoding>UTF-8</encoding>
    </configuration>
</plugin>
```

## 5. 风险与缓解

### 5.1 风险识别

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 依赖冲突 | 高 | 中 | 充分测试，提供依赖排除指南 |
| 反射性能开销 | 低 | 低 | 性能测试验证，缓存反射结果 |
| 未知 API 变更 | 中 | 中 | 全面测试矩阵，社区反馈 |
| Spring Boot 4.X 不兼容 | 高 | 低 | 预留适配接口，快速响应 |

### 5.2 回滚计划

如果 1.6.0 出现严重问题：
1. 保持 1.5.3 可用
2. 发布 1.6.1 修复版本
3. 提供降级指南

## 6. 验收标准

### 6.1 功能验收
- [ ] 在 Java 8 + Spring Boot 2.7.x 下正常工作
- [ ] 在 Java 17 + Spring Boot 3.2.x 下正常工作
- [ ] 在 Java 21 + Spring Boot 3.3.x 下正常工作
- [ ] 在 Java 25 + Spring Boot 4.X 下正常工作（如可用）
- [ ] tlog-example 项目在所有测试组合下运行正常

### 6.2 兼容性验收
- [ ] 1.5.3 用户可以无修改升级到 1.6.0
- [ ] 所有公开 API 保持不变
- [ ] 配置文件格式保持兼容

### 6.3 性能验收
- [ ] 日志输出性能退化 < 1%
- [ ] 启动时间增加 < 100ms

### 6.4 文档验收
- [ ] README 更新版本支持说明
- [ ] 提供升级指南
- [ ] 提供多版本配置示例

## 7. 时间估算

| 阶段 | 预计时间 | 依赖 |
|------|----------|------|
| Phase 1: 依赖升级 | 2-3 小时 | - |
| Phase 2: 适配层开发 | 3-4 小时 | Phase 1 |
| Phase 3: 自动配置适配 | 1-2 小时 | Phase 2 |
| Phase 4: 测试验证 | 4-6 小时 | Phase 3 |
| Phase 5: 文档更新 | 1-2 小时 | Phase 4 |
| **总计** | **11-17 小时** | |

## 8. 后续工作

### 8.1 tlog-example 项目更新

在 TLog 主项目完成后，更新 tlog-example：
1. 更新 TLog 依赖到 1.6.0
2. 创建多个测试配置（不同 Java + Spring Boot 组合）
3. 验证所有示例场景

### 8.2 持续维护

1. 监控 Spring Boot 4.X 发布，及时适配
2. 收集社区反馈，快速响应兼容性问题
3. 定期更新依赖版本

## 9. 附录

### 9.1 参考资料

- [Spring Boot 3.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Jakarta EE 9 Namespace Migration](https://jakarta.ee/specifications/platform/9/jakarta-platform-spec-9.html)
- [Maven Compiler Plugin Release Option](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#release)

### 9.2 关键文件清单

**需要修改的文件**：
- `/pom.xml` - 版本号和依赖管理
- `/tlog-spring-boot/*/pom.xml` - 子模块版本
- Servlet 相关类 - 适配 javax/jakarta
- 自动配置类 - 确保兼容性

**需要新增的文件**：
- `tlog-common/.../compat/CompatibilityDetector.java`
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `docs/UPGRADE_GUIDE.md`

---

**设计文档状态**: ✅ 完成，等待审核
