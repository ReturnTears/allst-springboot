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