# Spring Boot 集成 Docker

本模块是后续快速开发时可以直接照搬的 Docker 集成示例。应用本身仍是 Spring Boot 2.6.5、Java 11 的 Web 工程，Docker 只负责把已经打好的可执行 jar 做成镜像并运行。

参考的是 Spring 官方仓库，而不是网上零散的脚本：

- [spring-guides/gs-spring-boot-docker](https://github.com/spring-guides/gs-spring-boot-docker)：基础 `Dockerfile`、非 root 用户、`spring-boot:build-image`
- [Spring Boot 2.6 容器镜像](https://docs.spring.io/spring-boot/docs/2.6.5/reference/html/container-images.html)：分层 jar 和 `layertools`
- [Docker 多阶段构建](https://docs.docker.com/get-started/docker-concepts/building-images/multi-stage-builds/)：构建层不进入最终镜像

## 环境

| 项 | 本模块使用的版本 |
| --- | --- |
| JDK | 11 |
| Spring Boot | 2.6.5，版本来自父工程 `allst-springboot` |
| Maven | 3.6 及以上 |
| Docker | 20.10 及以上，并启用 Compose V2 |
| 基础镜像 | `eclipse-temurin:11-jre` |
| 服务端口 | `8099` |
| 本地镜像 | `allst/allst-springboot-docker:1.0-SNAPSHOT` |

`openjdk:11-jre-slim` 已经不再维护，所以镜像基座换成 Eclipse Temurin 11。Spring Boot 3 的启动类是 `org.springframework.boot.loader.launch.JarLauncher`；本模块是 2.6，启动类必须保持为 `org.springframework.boot.loader.JarLauncher`。

## 仓库里各文件的职责

| 文件 | 作用 |
| --- | --- |
| `Dockerfile` | 用分层 jar 构建运行镜像，进程以 `spring` 用户运行 |
| `.dockerignore` | 构建上下文只保留 `target/*.jar`，避免把源码、笔记和 `target` 里的其他产物送进 Docker |
| `docker-compose.yml` | 本地一条命令构建并启动，固定容器名、端口和 `docker` profile |
| `application.properties` | 端口、优雅停机、Actuator 健康检查 |
| `application-docker.properties` | 容器 profile，打开健康检查详情 |
| `pom.xml` 的 `docker` profile | 可选。用 Spotify `dockerfile-maven-plugin` 在 `mvn package` 时构建镜像 |

父 POM 在上一级目录 `allst-springboot/pom.xml`。如果在镜像里执行 `mvn package`，构建上下文里没有父 POM，Maven 会解析失败。因此这里固定为两段式：宿主机先打包，镜像只复制 jar。

## 集成步骤

下面的顺序就是以后新模块接入 Docker 时要重复的步骤。命令都在 `allst-springboot-docker` 目录执行。

### 1. 准备可执行 jar

`spring-boot-maven-plugin` 负责打 fat jar，并打开分层：

- `build-info` 生成构建信息，容器里可通过 `/actuator/info` 查看
- `repackage` 生成可执行 jar
- `<layers><enabled>true</enabled></layers>` 允许 `java -Djarmode=layertools` 拆出依赖层和应用层

依赖里增加 `spring-boot-starter-actuator`，用来提供 `/actuator/health/readiness`。`server.shutdown=graceful` 让 `docker stop` 时先处理完已接收的请求。

```text
mvn package -DskipTests
```

成功后得到 `target/allst-springboot-docker-1.0-SNAPSHOT.jar`。

### 2. 编写 Dockerfile

`Dockerfile` 分两个阶段：

1. `builder` 使用同一个 JRE 镜像，执行 `java -Djarmode=layertools -jar application.jar extract`。
2. 最终阶段按变化频率从低到高复制四层：`dependencies`、`spring-boot-loader`、`snapshot-dependencies`、`application`。依赖不变时，重构镜像会复用前面的层。

最终阶段还会：

- 安装 `curl`，只给 `HEALTHCHECK` 使用，安装完成后删除 apt 缓存
- 创建系统用户 `spring`，应用不再以 root 运行
- 暴露 `8099`
- 检查 `http://127.0.0.1:8099/actuator/health/readiness`

直接构建：

```text
docker build -t allst/allst-springboot-docker:1.0-SNAPSHOT .
```

### 3. 用 Compose 启动

`docker-compose.yml` 把宿主机 `8099` 映射到容器 `8099`，并设置：

- `SPRING_PROFILES_ACTIVE=docker`
- `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0`，让 JVM 按容器内存上限计算堆。Java 11 已默认打开 `UseContainerSupport`，不需要再加 `/dev/urandom`。

```text
docker compose up --build -d
```

已安装的是旧版独立命令时，把 `docker compose` 换成 `docker-compose`。

### 4. 验证

```text
docker compose ps
curl http://localhost:8099/docker/hello
curl http://localhost:8099/docker/info
curl http://localhost:8099/actuator/health/readiness
```

预期结果：

- `/docker/hello` 返回 `Hello Docker!`
- `/docker/info` 的 `profiles` 包含 `docker`，`hostname` 是容器主机名
- readiness 返回 `{"status":"UP"}`
- `docker compose ps` 里的状态在启动期之后变为 `healthy`

浏览器也可以直接打开这三个地址。

### 5. 停止和清理

```text
docker compose stop
docker compose start
docker compose down
docker rmi allst/allst-springboot-docker:1.0-SNAPSHOT
```

`docker compose down` 删除容器，不删除镜像。

## 日常开发时怎么选构建方式

三种方式打出来的镜像名保持一致，方便互相替换。

### 方式 A：Dockerfile，默认使用

适合要改基础镜像、用户、健康检查或 JVM 入口的时候。

```text
mvn package -DskipTests
docker compose up --build -d
```

不使用 Compose 时：

```text
docker build -t allst/allst-springboot-docker:1.0-SNAPSHOT .
docker run -d --name allst-springboot-docker -p 8099:8099 -e SPRING_PROFILES_ACTIVE=docker allst/allst-springboot-docker:1.0-SNAPSHOT
```

### 方式 B：Maven 的 docker profile

沿用原来的 Spotify `dockerfile-maven-plugin` 1.4.13，但只在显式打开 profile 时构建，并且不再自动 `push`。

```text
mvn -Pdocker package -DskipTests
```

等价镜像名是 `allst/allst-springboot-docker:1.0-SNAPSHOT`。

### 方式 C：Spring Boot Buildpacks

这是官方指南里的免 Dockerfile 路径。本机需要 Docker 守护进程。它不读取当前 `Dockerfile`，镜像内的用户、内存和入口由 Paketo Buildpacks 决定。

```text
mvn spring-boot:build-image -DskipTests
```

镜像名来自插件配置 `${docker.image.name}:${project.version}`，同样是 `allst/allst-springboot-docker:1.0-SNAPSHOT`。

## 改配置

| 目的 | 改哪里 |
| --- | --- |
| 改端口 | `application.properties` 的 `server.port`，同时改 `Dockerfile` 的 `EXPOSE`、`HEALTHCHECK` 和 `docker-compose.yml` 的端口映射 |
| 改本地镜像前缀 | `pom.xml` 的 `docker.image.prefix`，默认 `allst` |
| 推到原来的私有仓库 | `mvn -Pdocker package -Ddocker.image.prefix=10.0.75.0:5000`，镜像变为 `10.0.75.0:5000/allst-springboot-docker:1.0-SNAPSHOT`，然后再 `docker push` |
| 切换 Spring profile | `docker run -e SPRING_PROFILES_ACTIVE=dev -p 8099:8099 allst/allst-springboot-docker:1.0-SNAPSHOT` |
| 远程调试 | `docker run -e "JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,address=*:5005,server=y,suspend=n" -p 8099:8099 -p 5005:5005 allst/allst-springboot-docker:1.0-SNAPSHOT` |

以前 `dockerfile-maven-plugin` 绑定在默认生命周期上，每次打包都会向 `10.0.75.0:5000` 推送。现在默认 `mvn package` 只产生 jar。私有仓库地址改成参数，避免本机没有该仓库时构建失败。

## 接口

| 路径 | 说明 |
| --- | --- |
| `GET /` | 入口，列出下面几个地址 |
| `GET /docker/hello` | 固定返回 `Hello Docker!` |
| `GET /docker/info` | 应用名、端口、profile、主机名、Java 版本 |
| `GET /actuator/health` | 健康汇总 |
| `GET /actuator/health/liveness` | 存活探针 |
| `GET /actuator/health/readiness` | 就绪探针，镜像健康检查使用这个地址 |
| `GET /actuator/info` | Maven 构建信息 |

## 以后新模块接入时的最小清单

1. 父工程保持 Java 11 和 Spring Boot 2.6.x 时，直接复制 `Dockerfile`、`.dockerignore`、`docker-compose.yml`。
2. 把镜像名、容器名、`EXPOSE` 和端口映射改成新模块的名字和端口。
3. 确认启动类仍然是 `JarLauncher`。只有升级到 Spring Boot 3.2 及以上时，才改成 `org.springframework.boot.loader.launch.JarLauncher`，并把基础镜像换成对应的 Java 17 或 21。
4. 引入 Actuator，打开 `management.endpoint.health.probes.enabled=true`。
5. 先在宿主机 `mvn package`，再 `docker compose up --build`。不要把 Maven 构建放进镜像，除非构建上下文改成包含父 POM 的仓库根目录。
