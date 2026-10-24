<p align="center">
<a href="https://tlog.yomahub.com/">
    <img width="500" src="static/img/logo.png" alt="logo">
</a>
</p>

<h3>您的star是我继续前进的动力，如果喜欢请右上角帮忙点个star</h3>

## 概述
TLog是一个轻量级的分布式日志标记追踪神器，10分钟即可接入，自动对日志打标签完成微服务的链路追踪。支持log4j，log4j2，logback三大日志框架，支持dubbo，dubbox，springcloud三大RPC框架

## 项目官网请点击：[项目官网](https://tlog.yomahub.com/)

## 项目文档请点击：[项目文档](https://tlog.yomahub.com/pages/5b7bd2/)

## 示例工程请点击：[示例工程](https://gitee.com/bryan31/tlog-example)

## TLog 2.0

同一套 starter 支持 Spring Boot 2、3、4，运行产物为 Java 8 字节码；构建源码需要 JDK 17 或更新版本。具体运行组合与验收方法见 [兼容性测试](compatibility-tests/README.md)。

2.0 使用日志配置文件接入，已移除 javaagent 和 `AspectLogEnhance.enhance()`。1.x 用户请先阅读 [升级指南](docs/TLOG_2.0_UPGRADE.md)。

## 特性

* 日志标签与 MDC：支持 logback、log4j、log4j2，自定义标签模板和 traceId 生成器。
* Spring Boot 自动适配 javax/jakarta Servlet，覆盖同步、Callable 和 ASYNC/ERROR 重派发。
* Spring 管理的线程池自动传播 TLog 上下文和 MDC，保留已有 TaskDecorator；普通线程池可使用 `TLogExecutors.wrap`。
* 自动配置容器内的 RestTemplate、WebClient.Builder、RestClient.Builder（Spring 6.1+）。
* 支持 Dubbo、Dubbox、Feign、HttpClient 4/5、OkHttp、Hutool HTTP、Forest。
* WebFlux、Spring Cloud Gateway、Soul、Shenyu 使用 Reactor Context 隔离请求，在 Reactor 回调中恢复日志上下文。
* 支持 JDK TimerTask、Quartz、Spring Scheduled、XXL-JOB。
* Kafka、RabbitMQ 通过消息 header 透传：Spring Kafka / Spring AMQP 自动装配（`tlog.kafka.enabled`、`tlog.rabbitmq.enabled`），原生客户端各一行接入；其它 MQ 可继续使用 `TLogMqWrapBean` 包装消息体。
* 支持 W3C `traceparent` 入站 traceId 复用、总开关和各功能开关；提供 IDE 配置提示。

## 快速接入

引入 `com.yomahub:tlog-web-spring-boot-starter:2.0.0`，并保留应用原有的 Spring Boot Web 或 WebFlux starter。在日志 pattern 中加入 `%X{tl}`，或者使用 TLog 提供的 encoder/layout/converter。

完整配置、异步边界和迁移示例见 [升级指南](docs/TLOG_2.0_UPGRADE.md)，发布前验证命令见 [发布验收](docs/TLOG_2.0_RELEASE.md)。

由于社区群超过200人，需要邀请入群。关注公众号后点击`个人微信`加我，我可以拉你入群

![offIical-wx](static/img/offical-wx.jpg)

## 🦾赞助商

**Easysearch**

<a href="https://easysearch.cn/"><img src="https://infinilabs.cn/img/download/media-assets/easysearch-banner-4.webp"></a>

## 开源不易，支持就请赞助TLog

<img src="static/img/zanshang.jpeg" alt="zanshang" width="258" />