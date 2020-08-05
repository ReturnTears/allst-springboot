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
``` 