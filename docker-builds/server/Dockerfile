# syntax=docker/dockerfile:1.0.0-experimental
FROM clojure:openjdk-11-lein-2.9.5 AS builder
COPY . /namebazaar/
WORKDIR /namebazaar

# https://sanderknape.com/2019/06/installing-private-git-repositories-npm-install-docker/
RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan github.com >> ~/.ssh/known_hosts

# install dependencies and build contracts
RUN curl -fsSL https://deb.nodesource.com/setup_10.x | bash -
RUN apt-get install -y --no-install-recommends \
        build-essential \
        nodejs \
        python3.7 \
        python3-pip
RUN --mount=type=ssh,id=github lein deps
RUN lein npm install
RUN pip3 install solc-select
RUN solc-select install 0.5.17 && solc-select use 0.5.17
RUN lein compile-solidity

# build
RUN lein build-prod-server


FROM node:10-buster-slim
# ENV variables
ENV CONFIG /configs/namebazaar.config.edn

# get compiled JS
COPY --from=builder /namebazaar/server /namebazaar/server
COPY --from=builder /namebazaar/node_modules /namebazaar/node_modules
COPY --from=builder /namebazaar/resources /namebazaar/resources

WORKDIR /namebazaar

# expose server port
EXPOSE 3000
ENTRYPOINT ["node", "server/name-bazaar.js"]
CMD ["--max-old-space-size=2048"]