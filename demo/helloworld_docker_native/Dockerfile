FROM container-registry.oracle.com/graalvm/native-image:21-ol8 AS builder

COPY target/helloworld_docker_native-*.jar helloworld.jar

# Build
RUN native-image --no-fallback -jar helloworld.jar

# The deployment Image
FROM container-registry.oracle.com/os/oraclelinux:8-slim

EXPOSE 8080

# Copy the native executable into the containers
COPY --from=builder /app/helloworld helloworld
ENTRYPOINT ["/helloworld"]