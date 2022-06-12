# Spring Boot Basic
## Spring Boot 使用
```text
1、spring-boot-configuration-processor
   配置文件处理器，配置文件进行绑定就会有提示
   @ConfigurationProperties  @Value
   支持批量绑定               单个指定
   支持松散绑定(last-name / lastName)              不支持松散绑定
   不支持SpEL                支持SpEL
   支持JSR303数据校验         不支持
   支持复杂类型(map,list...)  不支持
默认从全局配置文件中获取值
@PropertySource 加载指定配置文件
@ImportResource 导入Spring配置文件,让配置文件里面的内容生效,需要标注在主启动类上

SpringBoot推荐注解的方式
@Configuration, 表明当前类是配置类，用于替代Spring配置文件，@Bean注解替代<bean>标签

2、Profile
application-{profile}.properties
注意.properties与.yml的格式区别
还可以在启动项目时配置环境变量(Programs arguments): --spring.profiles.active=dev
还可以在项目打包命令行运行的时候指定环境: java -jar xxx.jar --spring.profiles.active=prod
还可以在项目运行时配置虚拟机参数(VM options):-Dspring.profiles.active=test

3、配置文件加载位置
SpringBoot启动会扫描下面位置的application.properties或application.yml文件作为Spring Boot的默认配置文件
配置文件介绍
1)、application.properties
    扁平的K/v格式
2)、application.yml
    树形结构, k:(空格)v 表示一对键值对(空格必须有)
.properties与.yml同时存在时，系统默认的加载顺序，且这三个配置会形成互补配置
<includes>
  <include>**/application*.yml</include>
  <include>**/application*.yaml</include>
  <include>**/application*.properties</include>
</includes>

- file:./custom-config/
- file:custom-config/
- file:./config/
- file:./config/*/
- file:./
- classpath:/config/
- classpath:/
此顺序优先级从高到低, 所有位置的文件都会被加载，优先级高的配置内容会覆盖优先级低的配置内容，最终会形成互补配置
配置文件名称查找顺序是：.yml > .yaml > .properties
eg: java -jar xxx.jar --spring.profile.active=dev
也可以通过配置spring.config.location来改变默认配置,可以在项目打包好以后,通过命令行参数的形式来指定配置文件的新位置，指定配置文件会和默认配置共同起作用。但是不会和约定的配置文件进行互补
eg: java -jar xxx.jar --spring.config.location=E:\\TestData\\application.properties

4、外部配置加载顺序
1)、命令行参数
2)、jar包“外部”的application-{profile}.properties或者application.yml(带spring.profile)配置文件
2)、jar包”内部“的application-{profile}.properties或者application.yml(带spring.profile)配置文件
3)、jar包”外部“的application.properties或者application.yml(不带spring.profile)配置文件
4)、jar包”内部“的application.properties或者application.yml(不带spring.profile)配置文件
由jar包外部向内部进行寻找，带profile的优先加载

5、自动配置原理
官方文档:
https://docs.spring.io/spring-boot/docs/
https://docs.spring.io/spring-boot/docs/2.3.2.RELEASE/reference/html/

Importance>>
SpringBoot启动的时候, 加载主配置类，开启了自动配置功能@EnableAutoConfiguration
    @EnableAutoConfiguration注解的作用:
        利用AutoConfigurationImportSelector类给容器导入一些组件
        selectImports方法返回组件列表
        List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);返回候选配置
        List<String> configurations = SpringFactoriesLoader.loadFactoryNames(getSpringFactoriesLoaderFactoryClass(),
        				getBeanClassLoader());
        loadSpringFactories();
        Enumeration<URL> urls = classLoader != null ? classLoader.getResources("META-INF/spring.factories") : ClassLoader.getSystemResources("META-INF/spring.factories");
            扫描所有jar包路径下的META-INF/spring.factories,把扫描到的文件内容包装成properties对象，
            从properties中获取到EnableAutoConfiguration.class类对应的值，然后把他们添加在容器中
            # Auto Configure
            org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
            org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration,\
            org.springframework.boot.autoconfigure.aop.AopAutoConfiguration,\
            org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,\
            ...................此处省略一万字
        每一个这样的xxAutoConfiguration类都是容器中的一个组件，都加入到容器中，用他们来自动配置。
            每一个自动配置类进行自动配置功能
            以HttpEncodingAutoConfiguration为例进行解释自动配置原理:
            @Configuration(proxyBeanMethods = false)
            @EnableConfigurationProperties(ServerProperties.class)
            @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
            @ConditionalOnClass(CharacterEncodingFilter.class)
            @ConditionalOnProperty(prefix = "server.servlet.encoding", value = "enabled", matchIfMissing = true)
            public class HttpEncodingAutoConfiguration {}
            注解@Configuration表示这是一个配置类，可以给容器中添加组件
            注解@EnableConfigurationProperties表示启动指定类
            @ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
            public class ServerProperties {}
            在指定类中注解@ConfigurationProperties表示从配置文件中获取指定的值和bean的属性进行绑定
            
            注意：所有在配置文件中能配置的属性都是在xxxProperties类中封装着，配置文件可以配置什么就可以参照某功能对应的属性类。
            
            注解@ConditionalOnWebApplication表示Spring底层@conditional注解，根据不同的条件，如果满足指定的条件，整个配置类里面的配置就会生效。
            这里表示判断当前应用是否web应用，如果是当前配置类生效
            注解@ConditionalOnClass判断当前项目有没有这个类，CharacterEncodingFilter.class解决SpringMVC乱码的过滤器
            注解@ConditionalOnProperty判断配置文件中是否存在某个配置，server.servlet.encoding.enabled,如果不存在判断也是成立的，
                即使我们不配置server.servlet.encoding.enabled=true，默认也是生效的

自动配置的精髓:        
SpringBoot启动会加载大量的自动配置类
我们需要的功能在SpringBoot中有没有默认写好的自动配置类
自动配置类中配置了那些组件
给容器中自动配置类添加组建的时候，会从properties类中获取某些属性，就可以在配置文件中指定这些属性的值
xxxAutoConfiguration自动配置类
给容器添加组件
xxxProperties:封装配置文件中相关属性


6、SpringBoot devtools热部署
第一步：
<!-- 热部署依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
第二步：
IDEA中： File > Settings > Build,Execution,Deployment > Compiler > 勾选 build project Automatically
第三步：
IDEA中： 按住ctrl + alt + shift + / > 选择Registry  > 勾选 compiler.automake.allow.when.app.running 

7、日志框架
日志框架有那些？
日志门面(抽象层):”JCL(jakarta commons logging)“、Slf4j(Simple Logging Facade for Java)、”jboss-logging“
日志实现(实现层):log4j、 ”JUL(java.util.logging)“、”Log4j2“、logback
一般使用未带引号的框架，这三个框架出自同一作者
SpringBoot选择Slf4j和logback
在开发中，日志记录方法的调用，不应该直接调用日志的实现类，而是应该调用抽象层里面的方法
如何让系统中所有的日志都统一到slf4j?
1）、将系统中其他日志框架先排除
2）、用中间包替换原有的日志框架
3）、导入slf4j其他的实现
SpringBoot底层也是使用的slf4j+logback的方式进行日志记录
SpringBoot也把其他的日志框架替换为slf4j
新建的SpringBoot项目，已经默认配置了日志，默认是info级别的
默认配置:
1）、全局常规设置(格式， 路径， 级别)
2）、指定日志配置文件位置
3）、切换日志框架

指定配置：给类路径下放上每个日志框架自己的配置文件即可，SpringBoot就不使用自己默认的
logback > logback-spring.xml / logback-spring.groovy / logback.xml / logback.groovy
log4j2 > log4j2-spring.xml / log4j2.xml
JDK > logging.properties
官方推荐使用logback-spring.xml,因为直接使用logback.xml，该配置文件直接会被日志框架识别，
而使用logback-spring.xml，日志框架就不直接加载日志的配置项，由springboot解析日志配置，这样可以使用springboot的高级springProfile高级功能


8、访问项目下webjars的静态资源
https://www.webjars.org
http://localhost:8027/webjars/jquery/3.5.1/jquery.js
/**访问当前项目的任何资源
classpath:/META-INF/resources/
classpath:/resources/
classpath:/static/
classpath:/public/
/ 当前项目根路径
访问项目下自定义的js文件
http://localhost:8027/utils.js

欢迎页：静态资源文件夹下的所有的index.html页面，被“/**”映射
http://localhost:8027

所有的**/favicon.ico都在静态资源文件夹下找

thymeleaf模板引擎
th:标签
表达式语法:
${} OGNL表达式， 获取变量值，, 功能很强大， 查看官方文档
*{} 选择表达式 
#{} 获取国际化内容
@{} 定义url
~{} 片段引用表达式
%{} 


9、MockMvc
MockMvc是由Spring-test包提供， 实现了对Http请求的模拟，能够直接使用网络的形式转换到Controller的调用，使得测试速度快， 不依赖网络环境， 同时提供了一套验证的工具， 结果的验证十分方便。
在SpringBoot中测试

10、定制SpringMVC的自动配置
在大多数情况下,SpringBoot在自动配置中标记了很多@ConditionalOnMissingBean(xxx.class),我们只需要自定义其配置即可覆盖默认配置
``` 

## Spring Boot 简介
```text
有点：
-- 快速构建一个独立的Spring应用程序
-- 嵌入的Tomcat、Jetty或者Undertom，无需部署WAR文件
-- 提供starter POMs来简化Maven配置和减少版本冲突带来的问题
-- 对Spring和第三方库提供默认配置，也可以修改默认值，简化框架配置
-- 提供生产就绪功能，如指标，健康检查和外部配置
-- 无需配置XML，无代码生成，开箱即用

为什么是Spring Boot：
    SpringBoot简化了基于Spring开发,这只是最直观的一个方面，还有一个方面，也更得力于各微服务组件的支持，也就是谈SpringBoot必谈微服务的原因。
    可以说是SpringCloud带动了SpringBoot,SpringBoot成就了SpringCloud
    Spring Cloud >> Spring Boot >> Spring Framework

```

## Spring AOP
```text
Spring AOP 可以帮助我们在使用Java时不修改源代码的前提下实现功能的增强，
其底层实现就是基于Java动态代理或者CGLIB代理。

以往我们可以使用execution选定具体利用AOP扩展那些类，execution(* com.xx.xx...*Service.*(..))
这样使用非常不灵活，因为并不是Service中所有的方法都需要被增强
这时候我们可以Spring声明式注解，利用自定义注解来实现大量的共性需求（共性需求指的是通用的功能）。

应用场景：
1、收集上报指定关键方法的入参，执行时间，返回结果等关键信息，用作后期调优。
2、关键方法在幂等性前置校验，（基于本地消息表）
3、类似于Spring-Retry模块，提供关键方法多次调用重试机制
4、提供关键方法自定义的快速熔断，服务降级等职责
5、关键方法的共性入参校验
6、关键方法在执行后的扩展行为，例如：记录日志后，启动其他任务后等。

开发流程：
1、在SpringBoot项目中pom.xml文件引入aspectjweaver依赖
2、新建自定义注解，利用@interface关键字注解
3、
```