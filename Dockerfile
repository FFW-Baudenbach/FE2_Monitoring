FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY build/libs/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract


FROM eclipse-temurin:21-jre
LABEL maintainer="FFW Baudenbach <webmaster@ffw-baudenbach.de>"
EXPOSE 8080

# Set timezone
ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=Europe/Berlin
RUN apt-get update && apt-get install --no-install-recommends -y tzdata && apt-get clean && rm -rf /var/lib/apt/lists/*
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create user/group
RUN groupadd --gid 1001 appgroup && \
    useradd -rm -d /home/appuser -s /bin/bash -g appgroup -G sudo -u 1001 appuser

# Create and own directory
RUN mkdir /app && chown -R appuser:appgroup /app
USER appuser
WORKDIR /app

# Copy application from builder stage
COPY --chown=appuser:appgroup --from=builder /app/dependencies/ ./
COPY --chown=appuser:appgroup --from=builder /app/spring-boot-loader/ ./
COPY --chown=appuser:appgroup --from=builder /app/snapshot-dependencies/ ./
COPY --chown=appuser:appgroup --from=builder /app/application/ ./

# Set entrypoint
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]