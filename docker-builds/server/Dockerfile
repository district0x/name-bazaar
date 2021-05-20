# syntax=docker/dockerfile:1.0.0-experimental
FROM clojure:openjdk-11-lein-2.9.6-slim-buster AS builder
COPY . /namebazaar/
WORKDIR /namebazaar

# install base dependencies
RUN apt-get -yq update && apt-get install -yqq --no-install-recommends \
        build-essential \
        curl \
        git \
        python2.7 python-pip \
        python3.7 python3-pip \
        ssh

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

# install dependencies and build contracts
### python2 because of packages using old node-gyp
ENV PYTHON /usr/bin/python2
RUN --mount=type=ssh,id=github lein deps
RUN lein npm install

ENV PYTHON /usr/bin/python3
RUN pip3 install solc-select
RUN solc-select install 0.5.17 && solc-select use 0.5.17
RUN touch config.edn
# https://stackoverflow.com/questions/38323880/error-eacces-permission-denied
RUN npm install -g truffle --unsafe-perm=true --allow-root
RUN truffle compile

# build
RUN lein build-prod-server


FROM node:10-buster-slim
# ENV variables
ENV CONFIG /configs/namebazaar.config.edn
ENV PORT=3000

# get compiled JS
COPY --from=builder /namebazaar/server /namebazaar/server
COPY --from=builder /namebazaar/node_modules /namebazaar/node_modules
COPY --from=builder /namebazaar/resources /namebazaar/resources

WORKDIR /namebazaar

# expose server port
EXPOSE 3000

ENTRYPOINT ["node", "server/name-bazaar.js"]
CMD ["--max-old-space-size=2048"]
