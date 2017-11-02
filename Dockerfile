FROM "node:8"
MAINTAINER "Mike Konkov" <noospheratum@gmail.com>
EXPOSE 8080
RUN mkdir -p /usr/local/www
ADD server /usr/local/www/server
ADD node_modules /usr/local/www/node_modules
ADD resources /usr/local/www/resources
WORKDIR /usr/local/www
CMD ["node", "server/name-bazaar.js"]
# CMD ["bash"]