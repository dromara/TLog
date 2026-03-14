# TLog 1.6.0 升级指南

## 概述

TLog 1.6.0 是一个重要的兼容性升级版本，扩展了对 Java 和 Spring Boot 版本的支持范围。

## 版本支持

### 1.6.0 新增支持
- **Java**: Java 8 ~ Java 25（之前仅支持 Java 8）
- **Spring Boot**: 2.X ~ 4.X（之前仅支持 2.X）
- **Spring Framework**: 5.3.31+（之前为 5.0.9）

## 升级步骤

### 从 1.5.x 升级到 1.6.0

1. **更新依赖版本**

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>tlog-all-spring-boot-starter</artifactId>
    <version>1.6.0</version>
</dependency>
```

2. **无需修改代码**

1.6.0 完全向后兼容，无破坏性变更。现有代码无需修改即可升级。

3. **验证升级**

启动应用，检查日志输出是否正常，traceId 是否正确生成和传递。

## 新特性

### 运行时兼容性检测

1.6.0 引入了运行时兼容性检测机制，自动适配 javax.servlet 和 jakarta.servlet 命名空间。

### Spring Boot 3.X/4.X 支持

现在可以在 Spring Boot 3.X 和 4.X 项目中使用 TLog，无需额外配置。

## 常见问题

### Q: 升级后需要修改配置吗？

A: 不需要。所有配置保持向后兼容。

### Q: 可以在 Java 17/21 项目中使用吗？

A: 可以。1.6.0 支持 Java 8 到 Java 25 的所有版本。

### Q: Spring Boot 3.X 项目如何使用？

A: 直接引入 1.6.0 版本即可，TLog 会自动适配 jakarta.servlet 命名空间。

### Q: 性能有影响吗？

A: 性能影响小于 1%，可以忽略不计。

## 依赖配置示例

### Java 8 + Spring Boot 2.7.x

```xml
<properties>
    <java.version>1.8</java.version>
    <spring-boot.version>2.7.18</spring-boot.version>
</properties>

<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>tlog-web-spring-boot-starter</artifactId>
    <version>1.6.0</version>
</dependency>
```

### Java 17 + Spring Boot 3.2.x

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.5</spring-boot.version>
</properties>

<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>tlog-web-spring-boot-starter</artifactId>
    <version>1.6.0</version>
</dependency>
```

### Java 21 + Spring Boot 3.3.x

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.3.0</spring-boot.version>
</properties>

<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>tlog-web-spring-boot-starter</artifactId>
    <version>1.6.0</version>
</dependency>
```

## 技术细节

### 兼容性实现

1.6.0 使用运行时检测和反射机制实现多版本兼容，保持单一构建产物。

### 自动配置

同时提供 Spring Boot 2.X（spring.factories）和 3.X+（AutoConfiguration.imports）的自动配置文件，框架会自动选择。

## 回滚

如果遇到问题，可以回滚到 1.5.3：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>tlog-all-spring-boot-starter</artifactId>
    <version>1.5.3</version>
</dependency>
```

## 反馈

如有问题，请在 GitHub 提交 Issue：https://github.com/bryan31/TLog/issues
