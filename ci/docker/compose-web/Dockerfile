FROM ubuntu:22.04

SHELL ["/bin/bash", "-c"]

RUN echo "dash dash/sh boolean false" | debconf-set-selections
RUN dpkg-reconfigure -f noninteractive dash

RUN dpkg --add-architecture i386 && apt-get update -yqq && apt-get install -y \
    gnupg2 \
    openjdk-17-jdk \
    python3 \
    python-is-python3 \
    unzip \
    wget \
    xvfb


ARG CHROME_VERSION="google-chrome-stable"
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -

RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys A6DCF7707EBC211F \
    && echo "deb http://ppa.launchpad.net/ubuntu-mozilla-security/ppa/ubuntu focal main" >> /etc/apt/sources.list.d/mozilla.list \
    && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update -yqq \
    && apt-get install -y firefox ${CHROME_VERSION:-google-chrome-stable}

RUN wget https://github.com/mozilla/geckodriver/releases/download/v0.29.1/geckodriver-v0.29.1-linux64.tar.gz \
    && tar -xvzf geckodriver-v0.29.1-linux64.tar.gz && mkdir -p /root/.gradle/selenium/gecko  &&mv geckodriver /root/.gradle/selenium/gecko

RUN export CHROME_DRIVER_VERSION=$(wget -qO- https://chromedriver.storage.googleapis.com/LATEST_RELEASE) \
    && wget https://chromedriver.storage.googleapis.com/${CHROME_DRIVER_VERSION}/chromedriver_linux64.zip -P ~/tmp \
    && mkdir -p /root/.gradle/selenium/chrome &&  unzip -d /root/.gradle/selenium/chrome  ~/tmp/chromedriver_linux64.zip && rm ~/tmp/chromedriver_linux64.zip

COPY chrome-no-sandbox /usr/bin/chrome-no-sandbox 
RUN chmod u+x /usr/bin/chrome-no-sandbox
