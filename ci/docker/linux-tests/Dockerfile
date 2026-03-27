FROM ubuntu:24.04
ARG DEBIAN_FRONTEND=noninteractive

RUN apt update -y && \
    apt install -y \
        binutils \
        curl \
        dpkg \
        dpkg-dev \
        fakeroot \
        gnupg2 \
        libgl-dev \
        locales \
        maven \
        python-is-python3 \
        python3 \
        unzip \
        wget \
        xvfb \
        xz-utils && \
    locale-gen en_US.UTF-8 && \
    rm -rf /var/lib/apt/lists/*
ENV PATH="/usr/lib/x86_64-linux-gnu/:$PATH"

# Install OpenJDK 21
ARG JAVA_VERSION=21
RUN apt update -y && \
    apt install -y \
        openjdk-${JAVA_VERSION}-jdk && \
    rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-amd64
ENV PATH="$JAVA_HOME/bin:$PATH"

# Install NodeJS 22
RUN apt update -y && \
    wget -qO- https://deb.nodesource.com/gpgkey/nodesource.gpg.key && \
    wget -qO- https://deb.nodesource.com/setup_22.x | bash - && \
    apt install -y nodejs && \
    locale-gen en_US.UTF-8 && \
    rm -rf /var/lib/apt/lists/*

# Use UTF-8 by default
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

# Install JBR
ARG JBR_DISTR=jbr_jcef-21.0.8-linux-x64-b1163.62
ARG JBR_DISTR_ARCH=$JBR_DISTR.tar.gz
RUN wget https://cache-redirector.jetbrains.com/intellij-jbr/$JBR_DISTR_ARCH && \
    tar xzf $JBR_DISTR_ARCH && \
    rm $JBR_DISTR_ARCH && \
    mv $JBR_DISTR /usr/lib/jvm

# Install Android SDK
ENV ANDROID_HOME=/android/sdk
ARG CMD_TOOLS_VERSION=13114758
ARG CMD_TOOLS_ROOT=$ANDROID_HOME/cmdline-tools/$CMD_TOOLS_VERSION
ARG SDK_MANAGER=$CMD_TOOLS_ROOT/bin/sdkmanager
RUN mkdir -p $CMD_TOOLS_ROOT && \
    export CMD_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMD_TOOLS_VERSION}_latest.zip" && \
    wget $CMD_TOOLS_URL -O cmd-tools.zip && \
    unzip cmd-tools.zip && \
    rm cmd-tools.zip && \
    mv cmdline-tools/* $CMD_TOOLS_ROOT/
ARG ANDROID_PLATFORM=android-35
RUN yes | $SDK_MANAGER --licenses && \
    $SDK_MANAGER "platforms;$ANDROID_PLATFORM" && \
    cd $ANDROID_HOME/platforms/$ANDROID_PLATFORM && \
    ls -1 | grep -v android.jar | xargs rm -rf

# Install Google Chrome
ARG CHROME_VERSION="141.0.7390.122-1"
# https://googlechromelabs.github.io/chrome-for-testing/
ARG CHROME_DRIVER_VERSION="140.0.7339.82"
ARG CHROME_BIN=/usr/bin/google-chrome-stable
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys A6DCF7707EBC211F && \
    echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list && \
    apt update -y && \
    apt install -y google-chrome-stable=$CHROME_VERSION && \
    wget https://storage.googleapis.com/chrome-for-testing-public/$CHROME_DRIVER_VERSION/linux64/chromedriver-linux64.zip -P ~/tmp && \
    mkdir -p /root/.gradle/selenium/chrome && unzip -d /root/.gradle/selenium/chrome ~/tmp/chromedriver-linux64.zip && rm ~/tmp/chromedriver-linux64.zip && \
    rm -rf /var/lib/apt/lists/*

# Install Firefox
ARG FIREFOX_VERSION="142.0"
ARG GECKO_DRIVER_VERSION="0.36.0"
RUN wget -q -O /tmp/firefox.deb https://ftp.mozilla.org/pub/firefox/releases/${FIREFOX_VERSION}/linux-x86_64/en-US/firefox-${FIREFOX_VERSION}.deb && \
    dpkg -i /tmp/firefox.deb && \
    rm /tmp/firefox.deb && \
    apt --fix-broken install -y && \
    wget https://github.com/mozilla/geckodriver/releases/download/v$GECKO_DRIVER_VERSION/geckodriver-v$GECKO_DRIVER_VERSION-linux64.tar.gz && \
    tar -xvzf geckodriver-v$GECKO_DRIVER_VERSION-linux64.tar.gz && \
    mkdir -p /root/.gradle/selenium/gecko && \
    mv geckodriver /root/.gradle/selenium/gecko && \
    rm -rf /var/lib/apt/lists/*
