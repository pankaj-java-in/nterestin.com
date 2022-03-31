FROM openjdk:11-jre-slim

LABEL maintainer="Technologies"

RUN apt-get update

COPY target/kurento-group-call-6.16.0-exec.jar /opt/kurento-group-call-6.16.0-exec.jar

RUN md5sum /opt/kurento-group-call-6.16.0-exec.jar

ENTRYPOINT java -jar /opt/kurento-group-call-6.16.0-exec.jar