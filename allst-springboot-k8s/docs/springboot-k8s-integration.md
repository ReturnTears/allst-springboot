# Spring Boot 集成 Kubernetes

本模块是后续快速开发时可以直接照搬的 Kubernetes 集成示例。应用本身仍是 Spring Boot 2.6.5、Java 11 的 Web 工程，Kubernetes 做两件事：

1. 把这个应用部署进集群，并从 ConfigMap、Downward API 读取运行信息。
2. 在应用里用 Java 客户端访问 API Server，查询 Pod、Deployment、Service、ConfigMap，并在指定命名空间里维护 ConfigMap。

参考的是 GitHub 上的客户端仓库和 Kubernetes 官方的工作负载清单，而不是把示例代码原样拷进来：

- [fabric8io/kubernetes-client](https://github.com/fabric8io/kubernetes-client)：`KubernetesClientBuilder` 自动识别集群内 ServiceAccount 和本地 kubeconfig
- [Fabric8 Cheat Sheet](https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md)：Pod、Deployment、Service、ConfigMap 的 DSL
- [kubernetes-examples](https://github.com/fabric8io/kubernetes-client/tree/main/kubernetes-examples)：官方示例的资源覆盖范围。本模块只保留查询和一个受限的 ConfigMap 写入，没有照搬 exec、port-forward、删除任意 Pod
- [Spring Boot 2.6 容器镜像](https://docs.spring.io/spring-boot/docs/2.6.5/reference/html/container-images.html)：分层 jar
- [Deployments](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) 和 [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/)：`k8s/` 里的清单

没有使用 Spring Cloud Kubernetes。父工程的 Spring Cloud 是 `2021.0.3`，对应的 `spring-cloud-kubernetes` 2.1.x 绑定的是 Fabric8 5.x，并且这条线已经停止功能更新。直接使用 Fabric8 7.9，后续加接口时按 Cheat Sheet 扩展即可。

## 环境

| 项 | 本模块使用的版本 |
| --- | --- |
| JDK | 11 |
| Spring Boot | 2.6.5，版本来自父工程 `allst-springboot` |
| Maven | 3.6 及以上 |
| Fabric8 Kubernetes Client | 7.9.0 |
| Jackson | 2.20.0，只在本模块覆盖父工程的 2.13 |
| SnakeYAML | 2.2，只在本模块覆盖父工程的 1.29 |
| 基础镜像 | `eclipse-temurin:11-jre` |
| 服务端口 | `8100` |
| 业务命名空间 | `allst-demo` |
| 本地镜像 | `allst/allst-springboot-k8s:1.0-SNAPSHOT` |

Fabric8 8.0 要求 Java 17，并且换成 Jackson 3。当前父工程还是 Java 11，所以停在 7.9。这是 7.x 的最后一个功能版本。

父工程把 Jackson 管在 2.13、把 SnakeYAML 管在 1.29，并且 Spring Cloud `2021.0.3` 把 Fabric8 模型管在 5.x。只改 `kubernetes-client` 的版本会让 7.9 的客户端配上 5.x 的模型，启动测试时会找不到 `UnmatchedFieldTypeModule`。所以本模块额外导入 `kubernetes-client-bom` 7.9.0，再把 Jackson、SnakeYAML 覆盖到 Fabric8 7.9 需要的版本。这三处都写在本模块的 `dependencyManagement` 里，其他子模块不受影响。

因为 SnakeYAML 被抬到 2.x，而 Spring Boot 2.6.5 自带的 YAML 绑定还是按 1.x 写的，所以本模块配置只用 `application.properties`。不要把同一份配置改成 `application.yml`。

## 仓库里各文件的职责

| 文件 | 作用 |
| --- | --- |
| `pom.xml` | Web、Actuator、Fabric8 7.9。用 `kubernetes-client-bom` 盖过父工程里的 Fabric8 5.x，并覆盖 Jackson / SnakeYAML |
| `KubernetesClientConfig` | 创建客户端。创建失败时应用仍能启动 |
| `K8sQueryService` | 查询和受限的 ConfigMap 写入 |
| `K8sController` | `/k8s/**` HTTP 接口 |
| `application.properties` | 端口、优雅停机、探针、默认命名空间 |
| `Dockerfile` | 用分层 jar 构建运行镜像，进程以 `spring` 用户运行 |
| `k8s/` | Namespace、ConfigMap、RBAC、Deployment、Service，用 Kustomize 一次提交 |
| `src/test` | 不连接集群。检查命名空间限制、Pod 视图和 HTTP 入口 |

## 集成步骤

下面的顺序就是以后新模块接入 Kubernetes 时要重复的步骤。命令都在 `allst-springboot-k8s` 目录执行。

### 1. 引入客户端

依赖三块：

- `spring-boot-starter-web`：HTTP 接口
- `spring-boot-starter-actuator`：`/actuator/health/liveness` 和 `/actuator/health/readiness`，给容器和 Pod 探针用
- `io.fabric8:kubernetes-client:7.9.0`：访问 API Server

`spring-boot-maven-plugin` 打开 `build-info` 和 `repackage`，并启用分层 jar。父 POM 在上一级目录，镜像里不能再跑 Maven。

测试不连接真实集群。`K8sQueryServiceTest` 检查命名空间限制和 Pod 视图；`K8sControllerTest` 检查 HTTP 是否在客户端未就绪时仍能返回。父 POM 没有给 Surefire 配 JUnit 5，所以本模块把 `maven-surefire-plugin` 固定为 `2.22.2`。

### 2. 创建 KubernetesClient

`KubernetesClientBuilder.build()` 的选择顺序和 Fabric8 文档一致：

1. 设置了 `k8s.master-url` 时，连这个地址。自签名测试集群再把 `k8s.trust-certs` 设为 `true`。
2. 进程在 Pod 里时，存在环境变量 `KUBERNETES_SERVICE_HOST`，客户端使用该 Pod 的 ServiceAccount。
3. 其他情况读取 `KUBECONFIG`，没有该变量时读取 `~/.kube/config`。

`Config.autoConfigure(null)` 负责第 2、3 步。自动配置抛错、但已经填了 `k8s.master-url` 时，退回到只带这个地址的 `ConfigBuilder`。

客户端创建失败不会让 Spring 启动失败。`/k8s/info` 里的 `kubernetes.clientReady` 为 `false`，`message` 是失败原因。真正访问集群的接口返回 503。这样没有集群的机器上仍然可以改业务代码、跑不依赖集群的测试。

### 3. 区分读和写

查询使用调用方凭据能看到的命名空间。会修改集群的只有这两个接口，并且命名空间必须等于 `k8s.namespace`（默认 `allst-demo`）：

- `PUT /k8s/configmaps/{name}`
- `DELETE /k8s/configmaps/{name}`

`GET /k8s/pods/{name}/log` 也会读到容器输出，同样限制在这个命名空间，`tailLines` 最大 500。

名称只接受 Kubernetes DNS label：小写字母、数字和 `-`，长度不超过 63。ConfigMap 的 `data` 最多 32 个键，单个值最长 8192 字符。

### 4. 健康检查不要绑到 API Server

Pod 的存活和就绪探针打的是 Actuator：

- `/actuator/health/liveness`
- `/actuator/health/readiness`

这两项只表示进程还活着、已经能接请求。API Server 暂时不可达时，Pod 不会被探针杀掉。集群是否连通看 `/k8s/cluster`。

`server.shutdown=graceful` 配合 Deployment 的 `terminationGracePeriodSeconds: 30`。Kubernetes 发 SIGTERM 后，进程先停掉入口，再在 20 秒内处理已接收的请求。

### 5. 从集群注入配置

这是部署方向，和 Java 客户端无关。`k8s/configmap.yaml` 提供：

```text
DEMO_MESSAGE=hello from configmap
```

Spring Boot 按松散绑定把环境变量 `DEMO_MESSAGE` 映射到 `demo.message`。Deployment 用 `envFrom.configMapRef` 注入它，再用 Downward API 注入：

- `POD_NAME`：`metadata.name`
- `K8S_NAMESPACE`：`metadata.namespace`，绑定到 `k8s.namespace`

本地没有这两个环境变量时，使用 `application.properties` 里的默认值。`/k8s/info` 的 `message` 和 `pod` 用来确认注入是否生效。

### 6. 给 Pod 授权

集群内的客户端用的是 ServiceAccount `allst-k8s`，不是本机 kubeconfig。`k8s/rbac.yaml` 给它：

| 资源 | 范围 | 动词 |
| --- | --- | --- |
| pods、pods/log、services、configmaps | 命名空间 `allst-demo` | get、list、watch |
| configmaps | 命名空间 `allst-demo` | create、update、patch、delete |
| deployments | 命名空间 `allst-demo` | get、list、watch |
| namespaces | 集群 | get、list |

本地 `mvn spring-boot:run` 用的是 kubeconfig 里的当前用户，不走这个 ServiceAccount。当前用户本身要有对应权限，否则接口返回 401 或 403。

### 7. 构建镜像

```text
mvn package -DskipTests
docker build -t allst/allst-springboot-k8s:1.0-SNAPSHOT .
```

`Dockerfile` 分两个阶段。`builder` 执行 `java -Djarmode=layertools -jar application.jar extract`。最终阶段按变化频率复制 `dependencies`、`spring-boot-loader`、`snapshot-dependencies`、`application`。依赖不变时，重构镜像会复用前面的层。

Spring Boot 2.6 的启动类是 `org.springframework.boot.loader.JarLauncher`。进程用户是镜像里的 `spring`，Deployment 设置 `runAsNonRoot: true`，不再写死 uid，避免和镜像用户不一致。

本地 kind 或 minikube 不会拉取 `allst/*`。构建后把镜像装进集群：

```text
kind load docker-image allst/allst-springboot-k8s:1.0-SNAPSHOT
```

minikube 对应的命令是 `minikube image load allst/allst-springboot-k8s:1.0-SNAPSHOT`。Deployment 的 `imagePullPolicy` 是 `IfNotPresent`。

### 8. 提交清单

```text
kubectl apply -k k8s
kubectl -n allst-demo rollout status deployment/allst-springboot-k8s
kubectl -n allst-demo port-forward svc/allst-springboot-k8s 8100:8100
```

`kubectl apply -k k8s` 会创建命名空间、ConfigMap、ServiceAccount、Role、ClusterRole 以及 Deployment 和 Service。ClusterRoleBinding 需要当前用户具备集群级绑定权限。桌面 Kubernetes 或 minikube 的默认管理员用户可以。

Service 是 ClusterIP。集群外访问用 port-forward，不依赖 Ingress Controller。

## 本地运行

不部署进集群时：

```text
mvn test
mvn spring-boot:run
```

打开：

- http://localhost:8100/
- http://localhost:8100/k8s/info
- http://localhost:8100/k8s/cluster
- http://localhost:8100/actuator/health

Windows PowerShell 里 `curl` 是 `Invoke-WebRequest` 的别名。下面的写操作使用 `curl.exe`。

```text
curl.exe http://localhost:8100/k8s/pods?namespace=allst-demo
curl.exe -X PUT http://localhost:8100/k8s/configmaps/demo-config -H "Content-Type: application/json" -d "{\"data\":{\"color\":\"blue\"}}"
curl.exe http://localhost:8100/k8s/configmaps/demo-config
curl.exe -X DELETE http://localhost:8100/k8s/configmaps/demo-config
```

`PUT` 省略 `namespace` 时写入 `k8s.namespace`。写到别的命名空间会返回 403。

## HTTP 接口

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/` | 入口列表 |
| GET | `/k8s/hello` | 固定文本 |
| GET | `/k8s/info` | 应用名、`demo.message`、Pod 名、客户端是否创建成功。不访问 API Server |
| GET | `/k8s/cluster` | 集群版本。这是连通性检查 |
| GET | `/k8s/namespaces` | 命名空间列表 |
| GET | `/k8s/pods?namespace=` | Pod 列表。省略 namespace 时用 `k8s.namespace` |
| GET | `/k8s/pods/{name}/log?tailLines=` | 只允许 demo 命名空间，默认 100 行，最多 500 行 |
| GET | `/k8s/deployments?namespace=` | Deployment 列表 |
| GET | `/k8s/services?namespace=` | Service 列表 |
| GET | `/k8s/configmaps?namespace=` | ConfigMap 列表，只返回键 |
| GET | `/k8s/configmaps/{name}` | ConfigMap 内容 |
| PUT | `/k8s/configmaps/{name}` | 创建或替换 demo 命名空间中的 ConfigMap。正文是 `{"data":{"key":"value"}}` |
| DELETE | `/k8s/configmaps/{name}` | 删除 demo 命名空间中的 ConfigMap |

错误体统一为 `status`、`error`、`message`。API Server 返回的 HTTP 状态还会放在 `kubernetesStatus`。

## 以后往上加功能

新增一种资源时，沿用 `K8sQueryService` 里的写法：

```java
client.pods().inNamespace(namespace).list()
client.configMaps().inNamespace(namespace).resource(configMap).createOr(NonDeletingOperation::update)
```

`createOrReplace()` 在 Fabric8 7 里已经标记过时，用 `createOr(NonDeletingOperation::update)`：不存在就创建，已存在就更新。资源名、字段和等待条件以 [Cheat Sheet](https://github.com/fabric8io/kubernetes-client/blob/main/doc/CHEATSHEET.md) 为准。返回给 HTTP 的对象继续用精简视图，不要把 Fabric8 的完整模型直接序列化出去，字段太多，而且会带上集群内部对象。

需要监听资源变化时，再引入 Cheat Sheet 里的 `inform()` / SharedInformer，并在 Spring 销毁回调里关掉。当前 demo 每次请求现查，没有后台 watch。

需要继续调用的集群能力如果超出 `allst-demo` 的 Role，先改 `k8s/rbac.yaml`，再改服务代码。不要把 ServiceAccount 绑成 `cluster-admin`。

升级到 Java 17 和 Spring Boot 3 之后，再评估 Fabric8 8.0。那一次要同时换 Jackson 3 和 Vert.x 5，不是只改版本号。

## 常见问题

**`/k8s/info` 里 `clientReady` 为 false**

本机没有 `~/.kube/config`，也没有设置 `KUBECONFIG`，进程也不在 Pod 里。装一个本地集群，或用 `k8s.master-url` 指向已有的 API Server。应用其他接口仍可启动。

**接口返回 401 或 403**

本地运行时看当前 kubeconfig 用户。集群内运行时看 `allst-k8s` 这个 ServiceAccount。`/k8s/namespaces` 需要 ClusterRole `allst-k8s-namespaces`。

**Pod 一直 ContainerCreating 或 ImagePullBackOff**

镜像名是 `allst/allst-springboot-k8s:1.0-SNAPSHOT`，仓库里没有这张镜像。先在本机构建，再 `kind load` 或 `minikube image load`。

**改完 ConfigMap，`/k8s/info` 的 message 没变**

`envFrom` 的环境变量在容器启动时注入，不会热更新。修改 `allst-k8s-config` 后执行：

```text
kubectl -n allst-demo rollout restart deployment/allst-springboot-k8s
```

**端口冲突**

本模块使用 `8100`，旁边的 Docker 示例使用 `8099`，两个可以同时开。
