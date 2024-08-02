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
RUN apt-get update && apt-get install --no-install-recommends -y tzdata libcap2-bin && apt-get clean && rm -rf /var/lib/apt/lists/*
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Ability to run icmp ping commands as non-root
RUN setcap cap_net_raw+eip $JAVA_HOME/bin/java

# Create and own directory
RUN mkdir /app && chown -R 1000:1000 /app
USER appuser
WORKDIR /app

# Copy application from builder stage
COPY --chown=1000:1000 --from=builder /app/dependencies/ ./
COPY --chown=1000:1000 --from=builder /app/spring-boot-loader/ ./
COPY --chown=1000:1000 --from=builder /app/snapshot-dependencies/ ./
COPY --chown=1000:1000 --from=builder /app/application/ ./

# Set entrypoint
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]