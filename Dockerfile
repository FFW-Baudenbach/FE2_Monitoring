FROM eclipse-temurin:17-jdk AS builder
WORKDIR application
COPY build/libs/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract


FROM eclipse-temurin:17-jre
ENV TZ=Europe/Berlin
LABEL maintainer="FFW Baudenbach <webmaster@ffw-baudenbach.de>"
RUN groupadd --gid 3000 appgroup && \
    useradd -rm -d /home/appuser -s /bin/bash -g appgroup -G sudo -u 1000 appuser

# Ability to run icmp ping commands as non-root
RUN apt-get update && \
    apt-get install -y libcap2-bin && \
    apt-get clean && \
    rm -rf /var/cache/apt/lists
RUN setcap cap_net_raw+eip $JAVA_HOME/bin/java

RUN mkdir application && chown -R appuser:appgroup ./application
USER appuser
WORKDIR application

COPY --chown=appuser:appgroup --from=builder application/dependencies/ ./
COPY --chown=appuser:appgroup --from=builder application/spring-boot-loader/ ./
COPY --chown=appuser:appgroup --from=builder application/snapshot-dependencies/ ./
COPY --chown=appuser:appgroup --from=builder application/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
