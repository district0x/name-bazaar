# syntax=docker/dockerfile:1.0.0-experimental
# FROM clojure:openjdk-11-lein-2.9.6-slim-buster AS builder
FROM namebazaar_base:local AS builder
ARG BUILD_ENV="prod"
ENV BUILD_ENV=${BUILD_ENV}

COPY . /namebazaar/
WORKDIR /namebazaar

# install dependencies and build contracts
### python2 because of packages using old node-gyp
ENV PYTHON /usr/bin/python2
RUN --mount=type=ssh,id=github lein deps
RUN lein npm install

ENV PYTHON /usr/bin/python3
RUN touch config.edn
# https://stackoverflow.com/questions/38323880/error-eacces-permission-denied
RUN truffle compile

# build
RUN lein build-prod-server

FROM node:10-buster-slim
ARG BUILD_ENV="prod"
ENV BUILD_ENV=${BUILD_ENV}
# ENV variables
ENV CONFIG="/configs/namebazaar.config.${BUILD_ENV}.edn"
ENV PORT=6200

# update and install security patches
RUN apt-get update && apt-get upgrade -yqq \
    && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# get compiled JS
COPY --from=builder /namebazaar/server /namebazaar/server
COPY --from=builder /namebazaar/node_modules /namebazaar/node_modules
COPY --from=builder /namebazaar/resources /namebazaar/resources

WORKDIR /namebazaar


# expose server port
EXPOSE 6200

ENTRYPOINT ["node", "server/name-bazaar.js"]
CMD ["--max-old-space-size=4096"]
