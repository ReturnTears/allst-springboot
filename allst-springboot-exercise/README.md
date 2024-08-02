# Spring Boot

## 1、运行SpringBoot项目
+ 1、命令行窗口运行SpringBoot
```text
Windows下命令行窗口切换到项目的pom.xml相同目录下，运行:
mvn spring-boot:run
```

+ 2、使用IntelliJ IDEA的Terminal窗口
```text
运行：
mvn spring-boot:run
```

+ 3、使用Maven面板
```text
打开“Maven”面板，然后展开该面板中的“Plugins”树节点，再展开该节点下的“spring-boot”插件节点，
在该插件节点下的“spring-boot：run”上单击鼠标右键, 点击“Run Maven Build”
```

+ 4、使用Run Anything
```text
mvn spring-boot:run
```

+ 5、使用运行配置
```text
edit configuration
```

## MyBatis-Flex
```text
官方地址：https://mybatis-flex.com/
使用 MyBatis-Flex 作为 MyBatis 的增强框架进行代码开发，并不会影响原有的 MyBatis 的任何功能。

MyBatis-Flex XML分页功能
XML 分页参数解释：
${qwSql}: 传入的 QueryWrapper 生成的 where 部分的 SQL，带有 "where" 关键字
${pageOffset}: sql offset 的值
${pageSize}: 需要查询的数据量
${pageNumber}: 当前的页码
${dbType}: 当前用户配置的数据库类型，用户切换数据源也有可能造成数据库类型发生变化。我们可以通过不同的 dbType 来编写不同的 SQL，以适配不同的数据库类型。
```

## SpringBoot数据格式化
```text
Spring core.convert包是一个通用类型转换系统。它提供了统一的ConversionService API和强类型Converter SPI，用于实现从一种类型到另一种类型的转换逻辑。

WebMvcConfigurer 是 Spring MVC 提供的一个接口，用于定制 Spring MVC 的行为。在 Spring Boot 中，这个接口非常有用，因为它允许你在不编写额外配置类的情况下，对 Spring MVC 的各种功能进行扩展和配置。

WebMvcConfigurer 接口的功能
WebMvcConfigurer 接口提供了一系列的方法，可以用来配置 Spring MVC 的不同方面，包括但不限于：
1、视图解析器 (addViewControllers, configureViewResolvers)
2、静态资源处理 (addResourceHandlers)
3、拦截器 (addInterceptors)
4、消息转换器 (extendMessageConverters)
5、格式化支持 (addFormatters)
6、内容协商 (configureContentNegotiation)
7、异常处理 (configureHandlerExceptionResolvers)
要使用 WebMvcConfigurer，你需要创建一个实现了该接口的类，并且在类中重写你需要配置的方法。以下是一些示例：
```
```text
示例 1: 添加视图控制器
```
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/home").setViewName("home");
    }
}
```
```text
示例 2: 添加静态资源处理
```
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("classpath:/static/");
    }
}
```
```text
示例 3: 添加拦截器
```
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyInterceptor()).addPathPatterns("/**");
    }
}
```
```text
注意事项
选择性实现：你不需要实现 WebMvcConfigurer 接口中的所有方法，只需要实现你需要配置的方法即可。
覆盖默认行为：实现 WebMvcConfigurer 接口的方法时，要注意不要覆盖 Spring Boot 的默认配置。例如，在配置视图解析器时，你需要使用 addViewControllers 方法而不是直接实现 configureViewResolvers 方法。
自定义配置类：如果你需要更复杂的配置，可以考虑使用 @Configuration 类加上 @EnableWebMvc 注解来完全接管 Spring MVC 的配置。
总结
WebMvcConfigurer 是一个强大的工具，可以让你轻松地定制 Spring MVC 的行为，而无需编写大量的 XML 配置文件。它是 Spring Boot 中一个非常实用的功能，可以极大地提高开发效率。
```

## 编程式事务TransactionTemplate 
```text
注意事项

**异常处理**：在编程式事务中，你需要显式地处理异常，并在必要时调用`status.setRollbackOnly()`来回滚事务。
**事务传播行为**：在某些情况下，你可能需要配置事务的传播行为（如`Propagation.REQUIRED`、`Propagation.REQUIRES_NEW`等），
这可以通过`TransactionTemplate`的配置来实现。

通过这种方式，你可以更灵活地控制事务的开始和结束，但同时也需要更仔细地管理事务的边界和异常处理。
```

## AnnotationFormatterFactory接口
```java
/**
getFieldTypes：返回可以使用注释的字段类型，那些类型的字段上可以添加自定义的注解。
getPrinter：返回Printer以打印带注释字段的值。
getParser：返回一个Parser来解析带注释字段的值。
*/
public interface AnnotationFormatterFactory<A extends Annotation> {

  Set<Class<?>> getFieldTypes();

  Printer<?> getPrinter(A annotation, Class<?> fieldType);

  Parser<?> getParser(A annotation, Class<?> fieldType);
}
```

## Swagger3的长远注解
```text
@Api()
用于类，标识这个类是swagger的资源 ，主要用在controller类上，会在接口文档上显示当前类说明

@ApiOperation()
用于方法，在接口文档上面对接口进行说明，是swagger最主要的注解

@ApiParam()
用于方法，参数，字段说明，在方法上面对参数进行说明，会在接口文档上面显示参数的说明

@ApiModel()
用于类，表示对类进行说明，用于参数用实体类接收时，在接口文档上面会显示这个类里所有字段的说明 

@ApiIgnore()
用于类，方法，方法参数，表示这个方法或者类被忽略，即不会显示在接口文档里面

@ApiImplicitParam()
用于方法，表示单独的请求参数，多数时候可以用@ApiParm替代

@ApiImplicitParams() 
用于方法，包含多个 @ApiImplicitParam

@ApiResponses
同@ApiImplicitParams() ，用于方法，会在接口文档里面对当前接口返回的信息进行说明

```

## SQL语句
```text
-- oracle --
drop table customers;
CREATE TABLE customers (
   id number primary key ,
   name VARCHAR(50),
   email VARCHAR(50),
   address VARCHAR(100),
   city VARCHAR(50),
   country VARCHAR(50)
);
select * from customers a where a.id > 0;
```
