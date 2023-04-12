FROM ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update -y && \
    apt-get install binutils --no-install-recommends -y && \
    apt-get install fakeroot --no-install-recommends -y && \
    apt-get install dpkg --no-install-recommends -y && \
    apt-get install dpkg-dev --no-install-recommends -y && \
    apt-get install libgl-dev --no-install-recommends -y
ENV PATH="/usr/lib/x86_64-linux-gnu/:$PATH"
RUN apt-get install openjdk-17-jdk --no-install-recommends -y
