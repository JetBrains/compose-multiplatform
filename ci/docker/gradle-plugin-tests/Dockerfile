FROM ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update -y && \
    apt-get install binutils --no-install-recommends -y && \
    apt-get install fakeroot --no-install-recommends -y && \
    apt-get install dpkg --no-install-recommends -y && \
    apt-get install dpkg-dev --no-install-recommends -y && \
    apt-get install libgl-dev --no-install-recommends -y && \
    apt-get install curl --no-install-recommends -y && \
    apt-get install unzip --no-install-recommends -y && \
    apt-get install openjdk-17-jdk --no-install-recommends -y

ENV ANDROID_HOME=/android/sdk
ARG ANDROID_PLATFORM=android-34
ARG CMD_TOOLS_VERSION=11076708
ARG CMD_TOOLS_ROOT=$ANDROID_HOME/cmdline-tools/$CMD_TOOLS_VERSION
ARG SDK_MANAGER=$CMD_TOOLS_ROOT/bin/sdkmanager

RUN mkdir -p $CMD_TOOLS_ROOT && \
    curl -o cmd-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-${CMD_TOOLS_VERSION}_latest.zip" && \
    unzip cmd-tools.zip && \
    rm cmd-tools.zip && \
    mv cmdline-tools/* $CMD_TOOLS_ROOT/ && \
    yes | $SDK_MANAGER --licenses && \
    $SDK_MANAGER "platforms;$ANDROID_PLATFORM" && \
    cd $ANDROID_HOME/platforms/$ANDROID_PLATFORM && \
    ls -1 | grep -v android.jar | xargs rm -rf

ENV PATH="/usr/lib/x86_64-linux-gnu/:$PATH"
