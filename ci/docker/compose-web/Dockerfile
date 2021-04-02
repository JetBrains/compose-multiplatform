FROM ubuntu:20.10

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

SHELL ["/bin/bash", "-c"]

RUN dpkg --add-architecture i386 && apt-get update -yqq && apt-get install -y \
    curl \
    git \
    python3 \
    python-is-python3

RUN apt-get clean 

RUN mkdir ~/bin
ENV PATH=~/bin:$PATH

RUN curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
RUN chmod a+x ~/bin/repo
RUN mkdir androidx-main 
WORKDIR  androidx-main
RUN ~/bin/repo init -u https://android.googlesource.com/platform/manifest \
    -b androidx-main --partial-clone --clone-filter=blob:limit=10M
RUN ~/bin/repo sync
