# Mybatis-Plus

## sql脚本
```sql
use mybatis_plus;
show tables;
create table `book` (
    id int primary key auto_increment,
    name varchar(20) not null ,
    author varchar(20) not null,
    status int default 1 comment '1-上架，2-在管，3-借出，4-损坏，5-丢失，6-下架',
    create_time datetime default current_timestamp,
    update_time datetime default current_timestamp on update current_timestamp
) engine=innodb default charset=utf8mb4 comment='图书表';
select * from book;
```

# 配置文件敏感信息加密
```text
1、可使用现成的jar
<dependency>
    <groupId>com.github.ulisesbocchio</groupId>
    <artifactId>jasypt-spring-boot-starter</artifactId>
    <version>3.0.5</version>
</dependency>

在命令行窗口执行：
mvn jasypt:encrypt-value -D jasypt.encryptor.password="123456" -D jasypt.plugin.value="123456"  -D jasypt.encryptor.algorithm=PBEWITHHMACSHA512ANDAES_256
前面的123456是加密的私钥，后面的123456是明文密码

当你在使用 Jasypt 库加密数据时遇到这个错误，通常是因为 Java 安装中默认的加密强度限制导致的。为了解决这个问题，你需要安装 Java Cryptography Extension (JCE) 的 Unlimited Strength Jurisdiction Policy Files。

以下是解决此问题的一般步骤：

1. **下载 JCE 文件**：
   访问 Oracle 的官方网站下载适用于你的 Java 版本的 JCE 文件。对于 Java 11，你需要下载 "Java Cryptography Extension (JCE) Policy Files for JDK 11"。

2. **安装 JCE 文件**：
   - 对于 Windows：
     - 解压下载的文件。
     - 将解压后的 `local_policy.jar` 和 `US_export_policy.jar` 文件复制到 `%JAVA_HOME%\jre\lib\security` 目录下。
   - 对于 Linux 或 macOS：
     - 同样地，解压下载的文件。
     - 将 `local_policy.jar` 和 `US_export_policy.jar` 文件复制到 `$JAVA_HOME/jre/lib/security` 目录下。

3. **验证安装**：
   - 在命令行中运行以下命令来确认 JCE 高强度算法是否可用：
     ```shell
     java -cp path/to/jasypt.jar org.jasypt.intf.cli.JasyptPBEStandardCli -p test
     例如：
     java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI input="123456" password="123456" algorithm=PBEWithMD5AndDES
     ```
     如果没有错误，并且输出了加密后的字符串，说明 JCE 已正确安装。

4. **确保使用正确的 JVM 参数启动应用**：
   如果你在容器或 IDE 中启动应用程序，请确保使用包含增强策略文件的 Java 虚拟机实例。例如，在命令行中启动 Java 应用程序时，确保 `%JAVA_HOME%` 环境变量指向了包含增强策略文件的 Java 安装路径。

如果是在容器环境下部署应用，可能需要额外的步骤来确保容器中的 Java 安装包含了这些文件。这可能涉及到修改 Dockerfile 或者其他容器构建文件，以确保政策文件被复制到了容器内的适当位置。

请确保遵循所有适用的许可条款和出口控制法律，因为 JCE Policy Files 受到特定的许可限制。如果你的应用程序部署在全球范围内，请注意遵守相关国家的法律法规。


```