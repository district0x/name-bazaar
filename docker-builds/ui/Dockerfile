FROM debian:stretch-slim
MAINTAINER "Filip Bielejec" <filip@district0x.io>

RUN apt-get update -y \
    && apt-get install --no-install-recommends -y \
    -q wget unzip nginx

# replace nginx config
COPY docker-builds/ui/nginx.conf /etc/nginx/nginx.conf

# replace default server
COPY docker-builds/ui/default /etc/nginx/sites-available/default

# nginx config
COPY docker-builds/ui/namebazaar.io /etc/nginx/sites-available/namebazaar.io

# setup error page
RUN wget --no-check-certificate -O X0X.html https://raw.githubusercontent.com/district0x/X0X/master/X0X.html \
  && mv X0X.html /usr/share/nginx/html/X0X.html

# setup static server
RUN ln -s -f /etc/nginx/sites-available/namebazaar.io /etc/nginx/sites-enabled/namebazaar.io

# get compiled JS
# COPY resources/public /memefactory/resources/public/

# get MAINTENANCE page content
RUN wget --no-check-certificate -O master.zip https://github.com/district0x/name-bazaar-coming-soon/archive/master.zip \
        && unzip master.zip \
        && rm master.zip \
        && mkdir -p /namebazaar/resources/public/ \
        && mv -v name-bazaar-coming-soon-master/* /namebazaar/resources/public/

RUN ls /namebazaar/resources/public/

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
