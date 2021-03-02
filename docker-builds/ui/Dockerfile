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
RUN lein build-css && lein build-prod-ui


FROM debian:buster-slim
MAINTAINER "Filip Bielejec" <filip@district0x.io>

RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q wget unzip nginx

# replace nginx config
COPY --from=builder /namebazaar/docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

# replace default server
COPY --from=builder /namebazaar/docker-builds/ui/default /etc/nginx/sites-available/default

# nginx config
COPY --from=builder /namebazaar/docker-builds/ui/namebazaar.io /etc/nginx/sites-available/namebazaar.io

# setup error page
RUN wget --no-check-certificate -O X0X.html https://raw.githubusercontent.com/district0x/X0X/master/X0X.html \
  && mv X0X.html /usr/share/nginx/html/X0X.html

# setup static server
RUN ln -s -f /etc/nginx/sites-available/namebazaar.io /etc/nginx/sites-enabled/namebazaar.io

# get compiled JS
COPY --from=builder /namebazaar/resources/public /namebazaar/resources/public/

## get MAINTENANCE page content
#RUN wget --no-check-certificate -O master.zip https://github.com/district0x/name-bazaar-coming-soon/archive/master.zip \
#        && unzip master.zip \
#        && rm master.zip \
#        && mkdir -p /namebazaar/resources/public/ \
#        && mv -v name-bazaar-coming-soon-master/* /namebazaar/resources/public/

RUN ls /namebazaar/resources/public/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
