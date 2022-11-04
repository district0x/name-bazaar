FROM clojure:openjdk-11-lein-2.9.8-slim-buster AS env
WORKDIR /app
RUN apt-get update && \
    apt-get -yq update && apt-get install -yqq --no-install-recommends \
        build-essential \
        curl \
        git \
        python3.7 python3-pip python3-dev && \
    rm -rf /var/lib/apt/lists/*

FROM env AS env-with-extra-deps
ENV NODE_VERSION 10.23.1
ENV NODE_VERSION_FOR_TRUFFLE v18.1.0
RUN curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.2/install.sh | bash
RUN . /root/.nvm/nvm.sh \
    && nvm install "${NODE_VERSION}" \
    && nvm install "${NODE_VERSION_FOR_TRUFFLE}" \
    && nvm alias default "v${NODE_VERSION}"
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
RUN pip3 install -U --no-cache-dir pip wheel
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
ARG BUILD_ENV="prod"
# COPY . .
RUN . /root/.nvm/nvm.sh \
    && nvm use "${NODE_VERSION_FOR_TRUFFLE}" \
    && truffle compile \
    && nvm use "${NODE_VERSION}"
RUN lein build-css && lein "build-${BUILD_ENV}-ui"

FROM builder AS tests
RUN nginx -t

FROM nginx:alpine
EXPOSE 80
COPY --from=builder /app/docker-builds/ui/nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /app/docker-builds/ui/default /etc/nginx/conf.d/default.conf
COPY --from=builder /app/docker-builds/ui/namebazaar.io.conf /etc/nginx/conf.d/namebazaar.io.conf
COPY --from=builder /app/resources/public /namebazaar/resources/public
