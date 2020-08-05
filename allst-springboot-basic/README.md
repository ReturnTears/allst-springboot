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


``` 