FROM ubuntu:20.10

SHELL ["/bin/bash", "-c"]

RUN echo "dash dash/sh boolean false" | debconf-set-selections
RUN dpkg-reconfigure -f noninteractive dash

RUN dpkg --add-architecture i386 && apt-get update -yqq && apt-get install -y \
    git \
    gnupg2 \
    python3 \
    python-is-python3 \
    wget

ARG CHROME_VERSION="google-chrome-stable"

RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
RUN echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update -yqq && apt-get install -y \
    ${CHROME_VERSION:-google-chrome-stable}

RUN mkdir ~/bin
ENV PATH=~/bin:$PATH

RUN wget -P ~/bin https://storage.googleapis.com/git-repo-downloads/repo
RUN chmod a+x ~/bin/repo
RUN mkdir androidx-main 
WORKDIR  androidx-main
RUN ~/bin/repo init -u https://android.googlesource.com/platform/manifest \
    -b androidx-main --depth=1 --partial-clone --clone-filter=blob:limit=10M
RUN ~/bin/repo sync -c --no-tags --no-clone-bundle -j4

RUN mkdir -p /mnt/agent
RUN ln -sf /androidx-main/tools/ /mnt/agent/tools
RUN ln -sf /androidx-main/prebuilts/ /mnt/agent/prebuilts
RUN ln -sf /androidx-main/external /mnt/agent/external

RUN rm -rf /androidx-main/frameworks \
           /androidx-main/prebuilts/jdk/jdk11/darwin-x86 \
           /androidx-main/prebuilts/jdk/jdk8/darwin-x86

RUN apt-get clean 
