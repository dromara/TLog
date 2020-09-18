# 一.TLog能解决什么痛点

随着微服务盛行，很多公司都把系统按照业务边界拆成了很多微服务，在排错查日志的时候。因为业务链路贯穿着很多微服务节点，导致定位某个请求的日志以及上下游业务的日志会变得有些困难。

这时候很多童鞋会开始考虑上SkyWalking，Pinpoint等分布式追踪系统来解决，基于OpenTracing规范，而且通常都是无侵入性的，并且有相对友好的管理界面来进行链路Span的查询。

但是搭建分布式追踪系统，熟悉以及推广到全公司的系统需要一定的时间周期，而且当中涉及到链路span节点的存储成本问题，全量采集还是部分采集？如果全量采集，就以SkyWalking的存储来举例，ES集群搭建至少需要5个节点。这就需要增加服务器成本。况且如果微服务节点多的话，一天下来产生几十G上百G的数据其实非常正常。如果想保存时间长点的话，也需要增加服务器磁盘的成本。

当然分布式追踪系统是一个最终的解决方案，如果您的公司已经上了分布式追踪系统，那TLog并不适用。

**TLog提供了一种最简单的方式来解决日志追踪问题，它不收集日志，也不需要另外的存储空间，它只是自动的对你的日志进行打标签，自动生成TraceId贯穿你微服务的一整条链路。并且提供上下游节点信息。适合中小型企业以及想快速解决日志追踪问题的公司项目使用。**

**为此我为了TLog适配了三大日志框架，支持自动检测适配。支持dubbo，dubbox，spring cloud三大RPC框架，更重要的是，你的项目接入TLog，可能连十分钟就不需要 ：）**



# 二.项目特性

<font color="#21433d">**目前TLog的支持的特性如下：**</font>

* <font color="#21433d">**通过对日志打标签完成轻量级微服务日志追踪**</font>
* <font color="#21433d">**对业务代码无侵入式设计，使用简单，10分钟即可接入**</font>
* <font color="#21433d">**支持常见的log4j，log4j2，logback三大日志框架，并提供自动检测，完成适配**</font>
* <font color="#21433d">**支持dubbo，dubbox，springcloud三大RPC框架**</font>
* <font color="#21433d">**支持日志标签的自定义模板的配置，提供多个系统级埋点标签的选择**</font>
* <font color="#21433d">**天然支持异步线程的追踪**</font>
* <font color="#21433d">**几乎无性能损耗**</font>



让我们开始吧！



# 三.快速开始

TLog支持了springboot的自动装配，在springboot环境下，只需要以下两步就可以接入！
## 3.1 依赖

```xml
<dependency>
  <groupId>com.yomahub</groupId>
  <artifactId>tlog-all-spring-boot-starter</artifactId>
  <version>1.0.2</version>
</dependency>
```
**目前jar包已上传中央仓库，可以直接依赖到**



## 3.2 日志框架适配

只需要在你的启动类中加入一行代码，即可以自动进行探测你项目所使用的Log框架，并进行增强。

```java
@SpringBootApplication
public class Runner {

    static {AspectLogEnhance.enhance();}//进行日志增强，自动判断日志框架

    public static void main(String[] args) {
        SpringApplication.run(Runner.class, args);
    }
}
```

!> 因为这里是用javassist实现，需要在jvm加载对应日志框架的类之前，进行字节码增强。所以这里用static块。并且Springboot/Spring的启动类中不能加入log定义，否则会不生效。或者如果你是用tomcat/jboss/jetty等外置容器启动的，则参照`4.1 Log框架配置文件增强`



## 3.3 RPC框架的适配

在Springboot环境下，TLog会自动探测你用的RPC框架，自动进行适配。



## 3.4 最终效果

只需要以上这2步，就可以把springboot项目快速接入了

这里以dubbo+log4j为例，Consumer端代码

![1](media/1.png)

日志打印：

```
2020-09-16 18:12:56,748 [WARN] [TLOG]重新生成traceId[7161457983341056]  >> com.yomahub.tlog.web.TLogWebInterceptor:39
2020-09-16 18:12:56,763 [INFO] <7161457983341056> logback-dubbox-consumer:invoke method sayHello,name=jack  >> com.yomahub.tlog.example.dubbox.controller.DemoController:22
2020-09-16 18:12:56,763 [INFO] <7161457983341056> 测试日志aaaa  >> com.yomahub.tlog.example.dubbox.controller.DemoController:23
2020-09-16 18:12:56,763 [INFO] <7161457983341056> 测试日志bbbb  >> com.yomahub.tlog.example.dubbox.controller.DemoController:24
```



Provider代码：

![2](media/2.png)



日志打印：

```
2020-09-16 18:12:56,854 [INFO] <7161457983341056> logback-dubbox-provider:invoke method sayHello,name=jack  >> com.yomahub.tlog.example.dubbo.service.impl.DemoServiceImpl:15
2020-09-16 18:12:56,854 [INFO] <7161457983341056> 测试日志cccc  >> com.yomahub.tlog.example.dubbo.service.impl.DemoServiceImpl:16
2020-09-16 18:12:56,854 [INFO] <7161457983341056> 测试日志dddd  >> com.yomahub.tlog.example.dubbo.service.impl.DemoServiceImpl:17
```



可以看到，经过简单接入后，各个微服务之间每个请求有一个全局唯一的traceId贯穿其中，对所有的日志输出都能生效，这下定位某个请求的日志链就变得轻松了。



# 四.其他配置

## 4.1 Log框架配置文件增强

如果你的自动化日志探测失效或者你用的是外置容器，你需要针对你项目中的日志框架配置进行修改，修改方法也很简单。

### 4.1.1 Log4J配置文件增强

只需要把`layout`的实现类换掉就可以了

每个公司的Log4J的模板大同小异，这里只给出xml的例子

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">
<log4j:configuration>
    <appender name="stdout" class="org.apache.log4j.ConsoleAppender">
        <!--替换成AspectLog4jPatternLayout-->
        <layout class="com.yomahub.tlog.core.enhance.log4j.AspectLog4jPatternLayout">
            <param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss,SSS} [%p] %m  >> %c:%L%n"/>
        </layout>
    </appender>
    <appender name="fileout" class="org.apache.log4j.DailyRollingFileAppender">
        <param name="File" value="./logs/test.log"/>
        <!--替换成AspectLog4jPatternLayout-->
        <layout class="com.yomahub.tlog.core.enhance.log4j.AspectLog4jPatternLayout">
            <param name="ConversionPattern" value="%d{yyyy-MM-dd HH:mm:ss,SSS} [%p] %m  >> %c:%L%n"/>
        </layout>
    </appender>
    <root>
        <priority value="info" />
        <appender-ref ref="stdout"/>
        <appender-ref ref="fileout"/>
    </root>
</log4j:configuration>

```



### 4.1.2 Logback的配置文件增强

换掉`encoder`的实现类或者换掉`layout`的实现类就可以了

以下给出xml示例：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration debug="false">
    <property name="APP_NAME" value="logtest"/>
    <property name="LOG_HOME" value="./logs" />
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <!--替换成AspectLogbackEncoder-->
		<encoder class="com.yomahub.tlog.core.enhance.logback.AspectLogbackEncoder">
			  <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
		</encoder>
    </appender>
    <appender name="FILE"  class="ch.qos.logback.core.rolling.RollingFileAppender">
        <File>${LOG_HOME}/${APP_NAME}.log</File>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <FileNamePattern>${LOG_HOME}/${APP_NAME}.log.%d{yyyy-MM-dd}.%i.log</FileNamePattern>
            <MaxHistory>30</MaxHistory>
            <maxFileSize>1000MB</maxFileSize>
        </rollingPolicy>
        <!--替换成AspectLogbackEncoder-->
        <encoder class="com.yomahub.tlog.core.enhance.logback.AspectLogbackEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 日志输出级别 -->
    <root level="INFO">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="FILE" />
    </root>
</configuration>

```



### 4.1.3 Log4J2的配置文件增强

log4J2由于是通过插件形式实现的，log4J2有自动扫描插件的功能。所以无需对配置文件做任何更改就能生效。



## 4.2 日志标签模板自定义

TLog默认只打出traceId，以<$traceId>这种模板打出，当然你能自定义其模板。还能加入其它的标签头

你只需要在springboot的application.properties里如下定义：

```properties
tlog.pattern=[$preApp][$preIp][$traceId]
```

`$preApp` ：上游微服务节点名称

`$preIp`：上游微服务的IP地址

`$traceId`：全局唯一跟踪ID



这样日志的打印就能按照你定义模板进行打印



# 五.非Springboot项目接入

需要引入maven依赖
```xml
<dependency>
  <groupId>com.yomahub</groupId>
  <artifactId>tlog-all</artifactId>
  <version>1.0.2</version>
</dependency>
```
**目前jar包已上传中央仓库，可以直接依赖到**

## 5.1 dubbo & dubbox

如果你的RPC是dubbo或者dubbox，需要在spring xml里如下配置

```xml
<bean class="com.yomahub.tlog.web.TLogWebConfig"/>
```



## 5.2 Spring Cloud

如果你的RPC是spring cloud，需要在spring xml里如下配置

```xml
<bean class="com.yomahub.tlog.feign.filter.TLogFeignFilter"/>
```



## 5.3 自定义模板

如果你要自定义模板，需要在spring xml如下配置

```xml
<bean class="com.yomahub.tlog.context.TLogLabelGenerator">
    <property name="labelPattern" value="[$preApp][$preIp][$traceId]"/>
</bean>
```



# 六.联系作者

关注公众号回复`liteflow`即可加入讨论群

![offIical-wx](http://yomahub.com/images/offIical-wx.jpg)
