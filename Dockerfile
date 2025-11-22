FROM eclipse-temurin:21-jdk-noble AS builder
WORKDIR /app
COPY build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher

FROM eclipse-temurin:21-jre-noble
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
USER 1000
WORKDIR /app

# Copy application from builder stage
COPY --chown=1000:1000 --from=builder /app/application/dependencies/ ./
COPY --chown=1000:1000 --from=builder /app/application/spring-boot-loader/ ./
COPY --chown=1000:1000 --from=builder /app/application/snapshot-dependencies/ ./
COPY --chown=1000:1000 --from=builder /app/application/application/ ./

# Set entrypoint
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
