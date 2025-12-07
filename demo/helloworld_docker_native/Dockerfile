FROM container-registry.oracle.com/graalvm/native-image:21-ol8 AS builder

COPY target/helloworld_docker_native-*.jar helloworld.jar

# Build
RUN native-image --no-fallback -jar helloworld.jar

# 运行阶段：用 Alpine 最小镜像（仅 5MB）
FROM ubuntu:18.04
EXPOSE 8080

# Copy the native executable into the containers
COPY --from=builder /app/helloworld helloworld
ENTRYPOINT ["/helloworld"]