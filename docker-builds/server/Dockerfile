FROM namebazaar-base:latest AS builder
# COPY . .
RUN . /root/.nvm/nvm.sh \
    nvm use "${NODE_VERSION_FOR_TRUFFLE}" \
    truffle compile \
    nvm use "${NODE_VERSION}"
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
