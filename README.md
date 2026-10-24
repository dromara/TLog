<p align="center">
<a href="https://tlog.yomahub.com/en">
    <img width="500" src="static/img/logo.png" alt="logo">
</a>
</p>

<h3>Your star is my motivation to keep going, please help with a star in the top right corner if you like it</h3>

## Overview
TLog is a lightweight distributed log tagging tracker, accessible in 10 minutes, automatically tags logs to complete microservice link tracking. Support log4j, log4j2, logback three logging framework, support dubbo, dubbox, springcloud three RPC framework

## Project home page please click: [project home page](https://tlog.yomahub.com/)

## Project documentation please click: [project documentation](https://tlog.yomahub.com/pages/5b7bd2/)

## Sample project please click: [sample project](https://gitee.com/bryan31/tlog-example)

## TLog 2.0

The same starter coordinates support Spring Boot 2, 3 and 4. TLog artifacts target Java 8; building the source requires JDK 17 or newer. See the [compatibility tests](compatibility-tests/README.md) for the tested runtime combinations.

Version 2.0 uses logging configuration. The javaagent and `AspectLogEnhance.enhance()` integration methods have been removed. Read the [upgrade guide](docs/TLOG_2.0_UPGRADE.md) before migrating from 1.x.

## Features

* Log labels and MDC for logback, log4j and log4j2, with custom patterns and trace ID generators.
* Automatic javax/jakarta Servlet selection, including Callable and ASYNC/ERROR redispatch.
* Context and MDC propagation for Spring-managed executors, composing existing task decorators; `TLogExecutors.wrap` for other executors.
* Automatic instrumentation of managed RestTemplate, WebClient.Builder and RestClient.Builder (Spring 6.1+).
* Dubbo, Dubbox, Feign, HttpClient 4/5, OkHttp, Hutool HTTP and Forest adapters.
* Reactor Context isolation for WebFlux, Spring Cloud Gateway, Soul and Shenyu, restoring log context during Reactor callbacks.
* JDK TimerTask, Quartz, Spring Scheduled, XXL-JOB.
* Kafka and RabbitMQ propagate the context in message headers: auto-configured for Spring Kafka / Spring AMQP (`tlog.kafka.enabled`, `tlog.rabbitmq.enabled`), one line for the plain clients. Other brokers can keep wrapping the payload with `TLogMqWrapBean`.
* Inbound W3C trace ID reuse, independent feature switches and IDE configuration metadata.

## Getting started

Add `com.yomahub:tlog-web-spring-boot-starter:2.0.0` alongside your application's Spring Boot Web or WebFlux starter. Include `%X{tl}` in the logging pattern, or use a TLog encoder/layout/converter.

See the [upgrade guide](docs/TLOG_2.0_UPGRADE.md) for configuration and asynchronous boundaries, and [release validation](docs/TLOG_2.0_RELEASE.md) for verification commands.

Since the community group is over 200 people, you need to invite to join the group. Follow the public number and click `Personal WeChat` to add me, I can pull you into the group

![offIical-wx](static/img/offical-wx.jpg)

## Sponsors

**Easysearch**

<a href="https://easysearch.cn/"><img src="https://infinilabs.cn/img/download/media-assets/easysearch-banner-4.webp"></a>

Open source is not easy, please sponsor TLog if you support it

<img src="static/img/zanshang.jpeg" alt="zanshang" width="258" />