# syntax=docker/dockerfile:1.0.0-experimental
FROM clojure:openjdk-11-lein-2.9.6-slim-buster AS builder

# install base dependencies
RUN apt-get -yq update && apt-get install -yqq --no-install-recommends \
        build-essential \
        curl \
        git \
        python2.7 python-pip \
        python3.7 python3-pip \
        ssh \
        && apt-get clean \
        && rm -rf /var/lib/apt/lists/*

# https://sanderknape.com/2019/06/installing-private-git-repositories-npm-install-docker/
RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts

# install nvm with node and npm
ENV NODE_VERSION 10.23.1
ENV NVM_DIR /root/.nvm

RUN mkdir $NVM_DIR
RUN curl https://raw.githubusercontent.com/creationix/nvm/v0.38.0/install.sh | bash
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm use v${NODE_VERSION}
RUN . "$NVM_DIR/nvm.sh" && nvm alias default v${NODE_VERSION}
ENV PATH="${NVM_DIR}/versions/node/v${NODE_VERSION}/bin/:${PATH}"

# install truffle
ENV PYTHON /usr/bin/python3
RUN PYTHON=/usr/bin/python3 pip3 install --no-cache-dir solc-select
RUN solc-select install 0.5.17 && solc-select use 0.5.17
RUN npm install -g truffle --unsafe-perm=true --allow-root
