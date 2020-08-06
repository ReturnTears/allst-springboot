# Spring Boot Basic
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
- file:./config/
- file:./
- classpath:/config/
- classpath:/
此顺序优先级从高到低, 所有位置的文件都会被加载，优先级高的配置内容会覆盖优先级低的配置内容，最终会形成互补配置
也可以通过配置spring.config.location来改变默认配置,可以在项目打包好以后,通过命令行参数的形式来指定配置文件的新位置，指定配置文件会和默认配置共同起作用。
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

``` 