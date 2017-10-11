# Production deployment
This document describes production deployment of NameBazaar.
 
#### Config
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
  - sendgrid-api-key is needed to interact with public API of the [Sendgird](https://sendgrid.com/) email delivery service.
 
Configuration is picked up from the Node.js `CONFIG` ENV variable:
```sh
CONFIG='/etc/config/namebazaar.json' node dev-server/name-bazaar.js 2> /tmp/namebazaar_error.log
```

#### Logging
NameBazaar uses (ELK)(https://www.elastic.co/products) stack for logging:

  - Logstash: The server component of ELK that processes incoming logs.
  - Elasticsearch: Stores all of the logs.
  - Kibana: Web interface for searching and visualizing logs.
  - Filebeat: Log shipping agent.
##### Client server

On the server for which we want to gather logs for, we need to:

##### Install Filebeat:
Download and untar:
```sh
$ wget https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-5.6.2-linux-x86_64.tar.gz
$ tar xvzf filebeat-*.tar.gz
```

##### Change the config 

For example using nano: `$ nano /filebeat/filebeat.yml`.

In the watched logs section:
```yml
# Paths that should be crawled and fetched. Glob based paths.
  paths:
    - /tmp/namebazaar.log
    - /tmp/namebazaar_error.log
    #- /var/log/*.log
```
In the output section configure Filebeat to use SSL to forward the logs to the ELK server (matching certificate needs to be on the ELK server):
```yml
#output.logstash:
  # The Logstash hosts
  hosts: ["ELK_server_IP:5044"]
  bulk_max_size: 1024
  
  # Optional SSL. By default is off.
  # List of root certificates for HTTPS server verifications
  ssl.certificate_authorities: ["/etc/pki/tls/certs/logstash-forwarder.crt"]
```
Start Filebeat: 
```sh
$ ./filebeat -c filebeat.yml
```

#####  ELK Server

On the server with ELK stack we need to:

##### Load Filebeat Index Template into Elasticsearch. 
The index template will configure Elasticsearch to analyze incoming Filebeat log messages:

```ssh
$ curl -L -O https://artifacts.elastic.co/downloads/beats/beats-dashboards/beats-dashboards-5.6.2.zip
$ unzip beats-dashboards-*.zip
$ curl -X PUT 'http://localhost:9200/_template/filebeat?pretty' -d@filebeat-index-template.json
```

##### Configure Logstash

Using nano: `$ nano /etc/logstash/conf.d/logstash.conf `.
The certificate and key need to be generated and the ssl certficate need to match the one on the Client Server:

```conf
input {
  beats {
    port => 5044
    ssl => true
    ssl_certificate => "/etc/pki/tls/certs/logstash-forwarder.crt"
    ssl_key => "/etc/pki/tls/private/logstash-forwarder.key"
  }
}

filter { 
  date {
    match => [ "timestamp" , "yyyy-MM-dd HH:mm:ss,SSS" ]
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    manage_template => false
    document_type => "%{[@metadata][type]}"
    index => "filebeat-%{+YYYY.MM.dd}"
    }
  stdout { codec => rubydebug }
}
```
 You can now start Logstash, ElasticSearch and Kibana:
 
 ```sh
 $ logstash -f /etc/logstash/conf.d/logstash.conf
 $ elasticsearch -E network.host=localhost
 $ kibana
 ```

The web interface for Kibana runs on port `5601` of the ELK server.