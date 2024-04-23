FROM ubuntu:22.04

ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64
ENV PATH $JAVA_HOME/bin:$PATH

RUN apt update
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends xvfb openjdk-17-jdk
