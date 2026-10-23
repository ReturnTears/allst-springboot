# 分层镜像。父 POM 在上一级目录，镜像内不执行 Maven，只复制已经打好的 jar。
# 做法与 allst-springboot-docker 相同，依据 Spring Boot 2.6 容器文档。

FROM eclipse-temurin:11-jre AS builder
WORKDIR /application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:11-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system spring \
    && adduser --system --ingroup spring spring
WORKDIR /application
COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./
USER spring:spring
EXPOSE 8100
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8100/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
