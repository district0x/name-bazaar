FROM namebazaar-base:latest AS builder
ARG BUILD_ENV="prod"
# COPY . .
RUN . /root/.nvm/nvm.sh \
    nvm use "${NODE_VERSION_FOR_TRUFFLE}" \
    truffle compile \
    nvm use "${NODE_VERSION}"
RUN lein build-css && lein "build-${BUILD_ENV}-ui"

FROM builder AS tests
RUN nginx -t

FROM nginx:alpine
EXPOSE 80
COPY --from=builder /app/docker-builds/ui/nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /app/docker-builds/ui/default /etc/nginx/conf.d/default.conf
COPY --from=builder /app/docker-builds/ui/namebazaar.io.conf /etc/nginx/conf.d/namebazaar.io.conf
COPY --from=builder /app/resources/public /namebazaar/resources/public