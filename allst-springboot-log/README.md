# Log相关
```text
记录日志不能直接使用日志实现框架， 必须通过日志门面来实现

日志格式介绍:
2021-06-06 12:52:43.755 ERROR 972 --- [           main] com.allst.springboot.LogApplication      : error...
日期                      级别  pid 分隔符 [线程名称]  类名  ：   日志信息

springboot默认格式:
%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:m<n:ss.SSS}}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta)
%clr 当前内容颜色 {faint}
(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:m<n:ss.SSS}})为显示内容
springboot系统环境变量： LOG_DATEFORMAT_PATTERN默认为null
${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:m<n:ss.SSS}这里使用了三目运算:LOG_DATEFORMAT_PATTERN不为null则使用其格式

如何查看源码配置的默认系统参数：
第一步：配置文件中点击：logging.pattern.dateformat，进入到LoggingApplicationListener监听器中
第二步：找到onApplicationEvent方法，点击onApplicationEnvironmentPreparedEvent方法，找到initialize方法，找到apply方法
第三步：在apply方法的实现方法中找到环境变量设置参数
       this.setSystemProperty(resolver, "CONSOLE_LOG_PATTERN", "pattern.console")
       this.setSystemProperty(resolver, "LOG_DATEFORMAT_PATTERN", "pattern.dateformat");
       public static final String LOG_DATEFORMAT_PATTERN = "LOG_DATEFORMAT_PATTERN";
logging.pattern.dateformat=就是使用LOG_DATEFORMAT_PATTERN来设置系统参数

```

# LogBack中文网
```text
http://logback.cn

```

# Swagger相关
```text
pom引入jar，最新的3.0.0版本会报404, 解决思路：
1、依赖选择选择2.9.2
2、新建SwaggerConfig配置类,访问swagger-ui.html如果报404，
   需要配置类实现WebMvcConfigurer接口，WebMvcConfigurer接口实际上就是springBoot做拦击的一个抽象的接口，
   在springBoot早期的版本，使用的是WebMvcConfigurerAdapter抽象父类进行管理权限。然后在SwaggerConfig类重写addResourceHandlers方法，将不拦截的路径放入。

妥协了， 最后还是采用方法1解决404问题
```