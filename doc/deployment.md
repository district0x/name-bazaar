# Production deployment
This document describes production deployment of NameBazaar.
 
## Config

Create configuration file in JSON format, for example namebazaar.json:
```json
{"private-key" : "25615758538fef2b8a65aa",
 "public-key" : "256ebc161b4751583b3718e7",
 "sendgrid-api-key" : "SG.SM1ZLW7YREWS6I",
 "logging" : {"console" : true,
                 "file" : {"path" : "/tmp/namebazaar.log"}}}
```
  - public-key will be used to encode information on the UI.
  - matching private-key will be used to decode information on the backend server (**protect your production config private-key!**).
  - sendgrid-api-key is needed to interact with public API of the [Sendgrid](https://sendgrid.com/) email delivery service.
 
Configuration is picked up from the Node.js `CONFIG` ENV variable:
```sh
CONFIG='/etc/config/namebazaar.json' node dev-server/name-bazaar.js 2> /tmp/namebazaar_error.log
```

## Logging
NameBazaar uses [AWS ElasticSearchService](https://aws.amazon.com/elasticsearch-service/) (EK) stack for logging:
  - Elasticsearch: Stores all of the logs.
  - Kibana: Web interface for searching and visualizing logs.
  - Filebeat: Log shipping agent (runs on the client server).

###  Elasticsearch Server

#### Set access policy

From [AWS console](https://console.aws.amazon.com/console/home) create a new Elasticsearch Service instance.
When the instance is created select **Modify the access policy** and whitelist your client's server public IP:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": "es:*",
      "Resource": "ES_INSTANCE",
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": [
            "CLIENT_SERVER_IP"
          ]
        }
      }
    }
  ]
}
```

### Client server

#### Install Filebeat:
Download and untar:
```sh
$ wget https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-5.6.2-linux-x86_64.tar.gz
$ tar xvzf filebeat-*.tar.gz
```

#### Configure Filebeat

For example using nano: `$ nano /filebeat/filebeat.yml`.

In the watched logs section:
```yml
# Paths that should be crawled and fetched. Glob based paths.
  paths:
    - /tmp/namebazaar.log
    - /tmp/namebazaar_error.log
    #- /var/log/*.log
```

In the output section use https port (http port is 80) and forward the logs to the ES server:
```yml
#output.logstash:
  # The Logstash hosts
  hosts: ["ES_server_IP:443"]
  bulk_max_size: 1024
```

#### Load Filebeat Index Template into Elasticsearch. 

The index template will configure Elasticsearch to analyze incoming Filebeat log messages:

```ssh
$ curl -L -O https://artifacts.elastic.co/downloads/beats/beats-dashboards/beats-dashboards-5.6.2.zip
$ unzip beats-dashboards-*.zip
$ ./scripts/import_dashboards -only-index -dir beats-dashboards/filebeat/ -es ES_SERVER_IP:443
```

#### Start Filebeat: 

```sh
$ ./filebeat -e -d "publish" -c filebeat.yml
```

#### Kibana

We need to set a reverse proxy protected with nginx

Create a password file:

```sh
$ sudo sh -c "echo -n 'ADD_USERNAME:' >> /etc/nginx/.htpasswd"
```

Add an encrypted password entry:

```sh
$ sudo sh -c "openssl passwd -apr1 >> /etc/nginx/.htpasswd"
```

You can repeat this process for additional usernames/passwords.

Create nginx config file:

```sh
sudo nano /etc/nginx/logs.namebazaar.io
```

Create a server listening on port 443 and make sure that the following entry is there:

```json
location = / { rewrite ^ /_plugin/kibana/ redirect; }
location / {
    proxy_pass ES_SERVER_DOMAIN
    auth_basic "Restricted Content";
    auth_basic_user_file /etc/nginx/.htpasswd;
    proxy_http_version 1.1; 
    proxy_set_header Authorization ""; 
    proxy_hide_header Authorization; 
    proxy_set_header X-Forwarded-Proto $scheme; 
}
```

Create a softlink:

```sh
sudo ln -s -f /etc/nginx/sites-available/logs.district0x.io /etc/nginx/sites-enabled/logs.namebazaar.io
```

Check syntax:

```sh
sudo nginx -t
```

restart nginx:

```sh
sudo /etc/init.d/nginx restart
```
