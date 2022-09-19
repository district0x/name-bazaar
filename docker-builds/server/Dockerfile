FROM clojure:openjdk-11-lein-2.9.8-slim-buster AS env
WORKDIR /app
RUN apt-get update && \
    apt-get -yq upgrade && apt-get install -yqq --no-install-recommends \
        build-essential \
        curl \
        git \
        python3.7 python3-pip && \
    rm -rf /var/lib/apt/lists/*

FROM env AS env-with-extra-deps
ENV NODE_VERSION 10.23.1
ENV NODE_VERSION_FOR_TRUFFLE v18.1.0
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash
RUN . /root/.nvm/nvm.sh \
    && nvm install "${NODE_VERSION}" \
    && nvm install "${NODE_VERSION_FOR_TRUFFLE}" \
    && nvm alias default "v${NODE_VERSION}"
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
RUN pip3 install --no-cache-dir solc-select
RUN solc-select install 0.8.4 && solc-select use 0.8.4
RUN . /root/.nvm/nvm.sh \
    && nvm use "${NODE_VERSION_FOR_TRUFFLE}" \
    && npm install -g truffle \
    && nvm use "${NODE_VERSION}"

FROM env-with-extra-deps AS env-with-clj-deps
# here instead of "COPY . ." should be "COPY project.clj ." etc. for each file
# without this big part about cache and speed up build is lost
COPY . .
RUN lein deps
RUN lein npm i

FROM env-with-clj-deps AS builder
# COPY . .
RUN . /root/.nvm/nvm.sh \
    && nvm use "${NODE_VERSION_FOR_TRUFFLE}" \
    && truffle compile \
    && nvm use "${NODE_VERSION}"
RUN lein build-prod-server

FROM node:10-buster-slim
WORKDIR /namebazaar
ARG BUILD_ENV="prod"
EXPOSE 6200
ENV CONFIG="/configs/namebazaar.config.${BUILD_ENV}.edn"
ENV PORT=6200
RUN apt-get update && \
    apt-get upgrade -yqq && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/server /namebazaar/server
COPY --from=builder /app/node_modules /namebazaar/node_modules
COPY --from=builder /app/resources /namebazaar/resources

ENTRYPOINT ["node", "server/name-bazaar.js"]
CMD ["--max-old-space-size=2048"]