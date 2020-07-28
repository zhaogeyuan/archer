![logo](./doc/img/archer_logo_200.png)

#![coverage](https://img.shields.io/badge/coverage-80.5%25-brightgreen) ![version](https://img.shields.io/badge/version-1.0.0-ff69b4) 

- [介绍](#介绍)
- [入门](#入门)
    - [依赖](#依赖)
    - [注解的使用](#注解的使用)
    - [代理service](#代理service)
    - [使用redis或caffeine缓存实现](#使用redis或caffeine缓存实现)
    - [Spring配置](#Spring配置)
    - [SpringBoot配置](#SpringBoot配置)

# 介绍
archer是一个完全基于方法注解的缓存框架，解除缓存与业务代码的耦合。额外支持多对象缓存和列表缓存，从而提高缓存命中率：

- 开箱即用，支持最简化的配置
- 类上全局属性声明，减少方法注解的冗余配置
- 简明清晰的注解属性
- 基于map（默认）、redis、caffeine的缓存实现
- 基于Java（默认）、FastJson、Kryo、Hessian序列化方式
- 支持缓存穿透、击穿保护
- 组件高度可定制化
- JetBrains IDEA® 插件[SpEL-support](./plugin/archer-plugin.zip)，支持注解属性提示
- Spring及Spring Boot支持

依赖：

![jdk.version](https://img.shields.io/badge/Jdk-1.8%2B-ff69b4) 

![spring.version](https://img.shields.io/badge/Spring-4.0.0.RELEASE%2B-green) (Optional)

![spring.boot.version](https://img.shields.io/badge/Spring.boot-1.0.0.RELEASE%2B-blue) (Optional)

![jedis.version](https://img.shields.io/badge/Jedis-2.9.0%2B-red) (Optional)

![caffeine.version](https://img.shields.io/badge/Caffeine-2.8.4%2B-blueviolet) (Optional)

# 入门
## 依赖
```xml
<dependency>
    <groupId>com.atpexgo</groupId>
    <artifactId>archer-core</artifactId>
    <version>${archer.version}</version>
</dependency>
```
## 注解的使用
### 对象缓存
```java
interface UserService {

    @Cache(key = "'user:' + #userId", 
                expiration = 7, expirationTimeUnit = TimeUnit.DAYS, 
                breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
                valueSerializer = "customValueSerializer",
                keyGenerator = "customKeyGenerator",
                condition = "1 = 1",
                overwrite = false
        )
    User getUserById(long userId);
}
```
``key`` (支持 [SpringEL表达式](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html))  缓存key
``expiration``、``expirationTimeUnit`` 缓存存活时间(ttl)
``breakdownProtect`` 是否开启缓存击穿保护
``breakdownProtectTimeout``、``breakdownProtectTimeUnit`` 缓存击穿保护的超时时间，仅在开启缓存击穿保护时有效
``valueSerializer`` 自定义值序列化工具
``keyGenerator`` 自定义缓存key生成器，优先级高于``key``
``condition`` (支持 [SpringEL表达式](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html)) 缓存条件，仅当表达式值为true时进行缓存
``overwrite`` 是否覆盖缓存，开启时无论缓存是否存在都会被新值覆盖

⚠️注意：
所有的SpringEL表达式中的参数名称类似于``userId``需要在编译时通过jdk1.8的``-parameters``参数或者支持保留参数名的spring版本来支持，否则请使用参数序号如``arg0``或``param0``来表示


### 多对象缓存
```java
interface UserService {

    @CacheMulti(elementKey = "'user:' + #userIds$each",
                expiration = 7, expirationTimeUnit = TimeUnit.DAYS,
                breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
                valueSerializer = "customValueSerializer",
                keyGenerator = "customKeyGenerator",
                condition = "1 = 1",
                overwrite = false,
                orderBy = "#result$each.age"
        )
    List<User> getUsersByIdList(@MapTo("#result$each.id") List<Long> userIds);
}
```
``elementKey`` (支持 [SpringEL表达式](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html)) 结果每一个元素的缓存key
``orderBy`` (支持 [SpringEL表达式](https://docs.spring.io/spring/docs/4.2.x/spring-framework-reference/html/expressions.html)) 自定义结果的排序顺序

⚠️注意：
``#result``表示返回结果，``#result$each``表示数组或者集合的每一个元素


### 列表缓存