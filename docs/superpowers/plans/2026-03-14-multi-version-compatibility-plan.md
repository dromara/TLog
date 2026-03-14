# TLog 多版本兼容性升级实施计划

**版本**: 1.6.0
**创建日期**: 2026-03-14
**设计文档**: [2026-03-14-multi-version-compatibility-design.md](../specs/2026-03-14-multi-version-compatibility-design.md)

---

## 执行概览

本计划将 TLog 从 1.5.3 升级到 1.6.0，实现对 Java 8-25 和 Spring Boot 2.X-4.X 的支持。

**核心策略**: 单一构建产物 + 运行时适配 + 完全向后兼容

**预计总时间**: 11-17 小时

---

## Phase 1: 依赖升级与可选化

### 步骤 1.1: 更新根 pom.xml 版本和依赖

**文件**: `/pom.xml`

**操作**:
1. 更新项目版本: `1.5.3` → `1.6.0`
2. 更新 Spring Framework: `5.0.9.RELEASE` → `5.3.31`
3. 更新 maven-compiler-plugin: `3.0` → `3.11.0`
4. 添加 `<maven.compiler.release>8</maven.compiler.release>`
5. 将 Spring Boot 依赖标记为 `optional` + `provided`

**验证**:
```bash
mvn clean compile
```

---

### 步骤 1.2: 更新所有子模块 pom.xml

**文件**: 所有子模块的 `pom.xml`（继承父版本）

**操作**:
- 确认所有子模块正确继承父版本 1.6.0
- 检查是否有硬编码的版本号需要更新

**验证**:
```bash
mvn versions:display-dependency-updates
```

---

## Phase 2: 创建兼容性适配层

### 步骤 2.1: 创建 CompatibilityDetector

**文件**: `tlog-common/src/main/java/com/yomahub/tlog/compat/CompatibilityDetector.java`

**实现**:
```java
package com.yomahub.tlog.compat;

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

**验证**: 编译通过

---

### 步骤 2.2: 创建 Servlet 反射工具类

**文件**: `tlog-common/src/main/java/com/yomahub/tlog/compat/ServletReflectionUtils.java`

**实现**: 提供反射方法访问 Servlet API（javax/jakarta 通用）

**关键方法**:
- `getHeader(Object request, String name)`
- `addHeader(Object response, String name, String value)`
- `getAttribute(Object request, String name)`
- `setAttribute(Object request, String name, Object value)`

**验证**: 单元测试

---

### 步骤 2.3: 修改 TLogServletFilter 使用反射

**文件**: `tlog-webroot/src/main/java/com/yomahub/tlog/web/filter/TLogServletFilter.java`

**修改策略**:
- 将 `javax.servlet.*` 导入改为使用 Object 类型
- 使用反射工具类访问 Servlet API
- 保持业务逻辑不变

**验证**: 编译通过，功能测试

---

### 步骤 2.4: 修改 RequestWrapper 使用反射

**文件**: `tlog-webroot/src/main/java/com/yomahub/tlog/web/wrapper/RequestWrapper.java`

**修改策略**:
- 移除 `extends HttpServletRequestWrapper`
- 改为组合模式，内部持有原始 request
- 使用反射访问 Servlet API

**验证**: 编译通过，功能测试

---

### 步骤 2.5: 检查其他 Servlet API 使用

**搜索命令**:
```bash
grep -r "import javax.servlet" --include="*.java"
grep -r "import jakarta.servlet" --include="*.java"
```

**操作**: 逐个文件检查并适配

---

## Phase 3: Spring Boot 自动配置适配

### 步骤 3.1: 创建 Spring Boot 3.X 自动配置文件

**文件**: `tlog-spring-boot/tlog-web-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**内容**:
```
com.yomahub.tlog.springboot.lifecircle.TLogPropertyConfiguration
com.yomahub.tlog.springboot.TLogAspectAutoConfiguration
com.yomahub.tlog.springboot.TLogSpringScheduledAutoConfiguration
com.yomahub.tlog.springboot.TLogCommonAutoConfiguration
com.yomahub.tlog.springboot.TLogWebAutoConfiguration
```

**操作**: 为所有 starter 模块创建对应文件

---

### 步骤 3.2: 保留 Spring Boot 2.X 配置文件

**文件**: 保持现有 `META-INF/spring.factories` 不变

**验证**: 两个文件共存，Spring Boot 会自动选择

---

### 步骤 3.3: 检查条件注解兼容性

**文件**: 所有 `*AutoConfiguration.java`

**检查项**:
- `@ConditionalOnClass` 引用的类是否存在
- `@ConditionalOnProperty` 配置是否兼容
- `@ConditionalOnBean` 依赖是否正确

**验证**: 在不同 Spring Boot 版本下测试自动配置加载

---

## Phase 4: 测试验证

### 步骤 4.1: 编译测试（Java 8）

**命令**:
```bash
sdk use java 8.0.472-zulu
mvn clean install -DskipTests
```

**验证**: 编译成功，无警告

---

### 步骤 4.2: 编译测试（Java 17）

**命令**:
```bash
sdk use java 17.0.18-zulu
mvn clean install -DskipTests
```

**验证**: 编译成功

---

### 步骤 4.3: 编译测试（Java 21）

**命令**:
```bash
sdk use java 21.0.2-open
mvn clean install -DskipTests
```

**验证**: 编译成功

---

### 步骤 4.4: 编译测试（Java 25）

**命令**:
```bash
sdk use java 25.0.2-open
mvn clean install -DskipTests
```

**验证**: 编译成功

---

### 步骤 4.5: 单元测试

**命令**:
```bash
mvn test
```

**验证**: 所有测试通过

---

### 步骤 4.6: 集成测试 - Java 8 + Spring Boot 2.7.x

**位置**: `/Users/bryan31/openSource/tlog-example`

**操作**:
1. 更新 TLog 依赖到 1.6.0
2. 使用 Spring Boot 2.7.18（最新 2.X）
3. 运行所有示例场景
4. 手动验证日志追踪功能

**验证**: 功能正常，日志输出正确

---

### 步骤 4.7: 集成测试 - Java 17 + Spring Boot 3.2.x

**操作**:
1. 切换到 Java 17
2. 更新 Spring Boot 到 3.2.x
3. 运行所有示例场景
4. 验证 jakarta.servlet 命名空间工作正常

**验证**: 功能正常，无 ClassNotFoundException

---

### 步骤 4.8: 集成测试 - Java 21 + Spring Boot 3.3.x

**操作**:
1. 切换到 Java 21
2. 更新 Spring Boot 到 3.3.x
3. 运行所有示例场景

**验证**: 功能正常

---

### 步骤 4.9: 集成测试 - Java 25 + Spring Boot 4.X

**操作**:
1. 切换到 Java 25
2. 如果 Spring Boot 4.X 可用，更新并测试
3. 如果不可用，使用 Spring Boot 3.3.x 测试

**验证**: 功能正常

---

### 步骤 4.10: 性能测试

**测试内容**:
- 日志输出性能（使用现有 ContiPerf 测试）
- 启动时间对比

**验收标准**:
- 性能退化 < 1%
- 启动时间增加 < 100ms

---

## Phase 5: 文档更新

### 步骤 5.1: 更新 README.md

**文件**: `/README.md`

**更新内容**:
- 支持的 Java 版本: Java 8 ~ Java 25
- 支持的 Spring Boot 版本: 2.X ~ 4.X
- 版本号更新到 1.6.0

---

### 步骤 5.2: 创建升级指南

**文件**: `/docs/UPGRADE_GUIDE.md`

**内容**:
- 从 1.5.x 升级到 1.6.0 的步骤
- 依赖配置示例
- 常见问题解答

---

### 步骤 5.3: 更新依赖配置示例

**文件**: `/docs/DEPENDENCY_EXAMPLES.md`

**内容**:
- Java 8 + Spring Boot 2.X 配置
- Java 17 + Spring Boot 3.X 配置
- Java 21 + Spring Boot 3.X 配置
- 依赖排除指南

---

### 步骤 5.4: 更新 tlog-example 项目

**位置**: `/Users/bryan31/openSource/tlog-example`

**操作**:
1. 更新 TLog 依赖到 1.6.0
2. 创建多个 profile 配置不同版本组合
3. 更新 README 说明如何测试不同版本

---

## Phase 6: 发布准备

### 步骤 6.1: 创建 Git 提交

**命令**:
```bash
git add .
git commit -m "升级到 1.6.0: 支持 Java 8-25 和 Spring Boot 2.X-4.X

- 更新依赖版本和构建配置
- 添加运行时兼容性检测
- 适配 javax/jakarta 命名空间
- 同时支持 Spring Boot 2.X 和 3.X+ 自动配置
- 完全向后兼容，无破坏性变更

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### 步骤 6.2: 验收检查清单

**功能验收**:
- [ ] Java 8 + Spring Boot 2.7.x 正常工作
- [ ] Java 17 + Spring Boot 3.2.x 正常工作
- [ ] Java 21 + Spring Boot 3.3.x 正常工作
- [ ] Java 25 + Spring Boot 4.X 正常工作（如可用）
- [ ] tlog-example 所有场景测试通过

**兼容性验收**:
- [ ] 1.5.3 用户可无修改升级
- [ ] 所有公开 API 保持不变
- [ ] 配置文件格式兼容

**性能验收**:
- [ ] 性能退化 < 1%
- [ ] 启动时间增加 < 100ms

**文档验收**:
- [ ] README 更新完成
- [ ] 升级指南完成
- [ ] 配置示例完成

---

## 关键决策记录

### 决策 1: Servlet API 适配方式

**选择**: 使用反射而非适配器模式

**理由**:
- TLog 中 Servlet API 使用较简单
- 反射实现更轻量
- 避免维护两套适配器代码

---

### 决策 2: Spring 版本选择

**选择**: Spring Framework 5.3.31

**理由**:
- 兼容 Spring Boot 2.X 和 3.X
- 稳定版本，广泛使用
- 不需要用户修改依赖

---

### 决策 3: 自动配置文件策略

**选择**: 同时提供 spring.factories 和 AutoConfiguration.imports

**理由**:
- Spring Boot 会自动选择正确的文件
- 无需运行时检测
- 零配置，用户无感知

---

## 风险缓解措施

### 风险 1: 依赖冲突

**缓解**:
- 所有 Spring Boot 依赖标记为 optional + provided
- 提供详细的依赖排除指南
- 充分测试不同版本组合

---

### 风险 2: 反射性能开销

**缓解**:
- 性能测试验证影响
- 缓存反射 Method 对象
- 日志框架本身性能开销更大，反射影响可忽略

---

### 风险 3: 未知 API 变更

**缓解**:
- 全面测试矩阵覆盖
- 社区反馈快速响应
- 保持 1.5.3 可用作为回退

---

## 执行检查点

### 检查点 1: Phase 1 完成后

**验证**:
- [ ] 所有 pom.xml 更新完成
- [ ] Maven 编译成功
- [ ] 依赖树正确

---

### 检查点 2: Phase 2 完成后

**验证**:
- [ ] CompatibilityDetector 工作正常
- [ ] Servlet 反射工具类测试通过
- [ ] 所有 Servlet 相关代码适配完成

---

### 检查点 3: Phase 3 完成后

**验证**:
- [ ] 自动配置文件创建完成
- [ ] Spring Boot 2.X 和 3.X 都能加载配置

---

### 检查点 4: Phase 4 完成后

**验证**:
- [ ] 所有 Java 版本编译通过
- [ ] 所有测试组合验证通过
- [ ] 性能测试达标

---

### 检查点 5: Phase 5 完成后

**验证**:
- [ ] 所有文档更新完成
- [ ] tlog-example 项目更新完成

---

## 后续工作

1. 监控社区反馈，快速响应兼容性问题
2. 关注 Spring Boot 4.X 正式发布，及时适配
3. 定期更新依赖版本
4. 考虑添加自动化 CI 测试多版本组合

---

**计划状态**: ✅ 已完成，准备执行
