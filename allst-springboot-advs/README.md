# Spring Boot Advs
## 整合Mybatis
```text
http://localhost:8030/druid/login.html
```
## 缓存
```text
JSR107
如果要使用JSR107: 需要导入:
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
</dependency>

重点::
Spring缓存抽象
Application > CachingProvider > CacheManager > Cache > Entry<K, V> > Expiry
应用            缓存提供者            缓存管理器    缓存组件               过期    

importance:
Cache           缓存接口、定义缓存操作。实现由：RedisCache、EhcacheCache、ConcurrentMapCache等
                Cache缓存的实现不同， 它的实现技术就不同
CacheManager    缓存管理器、管理各种缓存Cache组件
@Cacheable      主要正对方法配置、能够根据方法的请求参数对其结果进行缓存
@CacheEvict     清空缓存
@CachePut       保证方法被调用、又希望结果被缓存,常用于缓存更新
@EnableCaching  开启基于注解的缓存
KeyGenerator    缓存数据时key生成策略
serialize       缓存数据value序列化策略

缓存使用步骤:
1、开启基于注解的缓存
2、标注缓存注解即可
   @Cacheable 将方法的结果进行缓存、以后再查询相同的数据，直接从缓存中获取即可，不再调用查询方法
   CacheManager管理多个cache组件，对缓存的真正CRUD操作是在cache组件中， 每个缓存组件又自己的唯一一个名字
   常见属性：
        cacheNames/value: 指定缓存组件的名字
        key, 指定缓存数据时的key, 默认使用方法参数的值，
        keyGenerator: key的生成器， 可以自己指定key的生成器组件id
                      key/keyGenerator二选一
        cacheManager: 指定缓存管理器，或者cacheResolver指定缓存解析器
        condition: 指定符合条件的情况才缓存
        unless否定缓存， 当unless指定的条件为true,方法的返回值就不会被缓存
        sync: 是否使用异步模式
    Cache SpEL available metadata
    名字               位置          描述                 示例
    methodName      root object     当前被调用的方法名   #root.methodName
    method          root object     当前被调用的方法    #root.method.name
    target          root object     当前被调用的目标对象      #root.target
    targetClass     root object     当前被调用的目标对象类     #root.targetClass
    args            root object     当前被调用的方法的参数列表   #root.args[0]
    caches          root object     当前方法调用使用的缓存列表(如@Cacheable(value={"cache1", "cache2"})),则有两个cache  #root.caches[0].name
    argument name   evaluation context  方法参数的名字，可以直接#参数名，也可以使用#p0或#a0的形式，0代表参数的索引   #iban   #a0 #p0
    result          evaluation context  方法执行后的返回值(仅当方法执行之后的判断有效，如“unless”，“cache put”的表达式，“cache evict”的表达式beforeInvocation=false)  #result
缓存运行流程:
@Cacheable
1、方法运行前，先查询cache(缓存组件)，按照cacheNames指定的名字获取，第一次获取缓存如果没有cache则会自动创建
2、去cache中查找缓存内容，使用一个key,默认是方法的参数，key是按照某种策略生成的，默认使用keyGenerator生成的，
3、没有查询到缓存就调用目标方法
4、将目标方法返回的结果放进缓存中

@CachePut
1、先调用目标方法
2、将目标方法的结果缓存起来

@CacheEvict
key 要清除的数据
allEntries = true 清除这个缓存的所有数据
beforeInvocation = false 缓存的清除是否在方法之前执行
    默认代表缓存清除操作是在方法执行之后执行， 如果出现异常缓存就不会被清除
    true表示缓存清除操作是在方法执行之前执行， 无论方法是否出现异常，缓存都会被清除

@Caching指定多个复杂规则

@CacheConfig(cachesName = "emp") 
抽取缓存的公共配置
```

## 整合redis作为缓存中间件
```text

```

## 消息队列
```text
JMS
AMQP 高级消息队列协议
消息队列的应用场景: 应用解耦、异步处理、流量削峰

1、大多数应用中， 可通过消息服务中间件来提升系统异步通信、扩展解耦能力
2、消息服务中两个概念:
   消息代理(message broker)、目的地(destination)
   当消息发送者发送消息以后，将由消息代理接管，消息代理保证消息传递到指定目的地
3、消息队列主要有两种形式的目的地
    1)队列:点对点消息通信
    2)主题:发布/订阅 消息通信

RabbitMQ
是一个由erlang开发的AMQP的开源实现
核心概念:
Message
消息是不具名的，它由下下哦i头和消息体组成。 消息头是不透明的，而消息头则由一系列的可选属性组成，
这些属性包括routing-key (路由键)、priority(相对于其他消息的优先权)、delivery-mode(指出)
该消息可能需要持久性存储

Publisher
消息的生产者，也是一个向交换器发布消息的客户端应用程序

Exchange
交换器，用来接收生产者发送的消息并将这些消息路由给服务器中的队列
Exchange有四种类型：direct(默认)、fanout、topic、header， 不同类型的Exchange转发消息的策略有所区别

Queue
消息队列， 用来保存消息直到发送给消费者。它是消息的容器，也是消息的终点，一个消息可投入一个或多个队列。
消息一直在队列里面，等待消费者连接到这个队列将其取走

Binding
绑定，用于消息队列和交换器之间的关联。一个绑定就是基于路由键将交换器和消息队列连接绑定起来的路由规则
所以可以将交换器理解成一个由绑定构成的路由表
Exchange和queue的绑定可以是多对多的关系

Connection
网络连接， 比如TCP连接

Channel
信道， 多路复用连接中的一条独立的双向数据流通道。信道是建立在真实的TCP连接内的虚拟连接，
AMQP命令都是通过信道发出去的， 不管是发布消息，订阅队列还是接收消息，这些动作都是通过信道完成的。
因为对于操作系统来说简历TCP连接代价是昂贵的，所以引入信道的概念， 以复用一条TCP连接

Consumer
消息消费组， 表示一个消息队列中取得消息的客户端应用程序

Virtual Host
虚拟主机， 表示一批交换器、消息队列和相关对象。虚拟主机是共享相同的身份认证和加密环境的独立服务器域。
每个vhost本质上就是一个mini版的RabbitMQ服务器， 拥有自己的队列， 交换器， 绑定和权限机制。
vhost是AMQP概念的基础， 必须在连接时指定RabbMQ默认的vhost是 / 

Broker
表示消息队列服务器实体

RabbitMQ运行机制
AMQP中消息路由
AMQP中消息的路由过程和Java开发者熟悉的JMS存在一些差异， AMQP中增加了Exchange和Binding的角色。
生产者把消息发布到Exchange上， 消息最终到达队列并被消费者接收， 而Binding决定交换器的消息应该发送到那个队列

```
## &任务
```text
异步任务
@Async 
@EnableAsync

定时任务
Spring为我们提供了异步执行任务调度的方式，TaskExecutor,TaskScheduler接口
注解：@EnableScheduling @Scheduled
corn表达式:
字段        允许值                   允许的特殊字符
秒          0-59                     ,-*/
分          0-59                     ,-*/
小时         0-23                     ,-*/
日期         1-31                     ,-*?/LWC
月份         1-12                     ,-*/
星期         0-7或SUN-SAT 0,7是SUN     ,-*?/LC#
特殊字符代表的含义:
, 枚举
- 区间
* 任意
/ 步长
? 日/星期冲突匹配
L 最后
W 工作日
C 和calendar联系后计算过的值
# 星期，4#2，第2个星期四

邮件任务

```
