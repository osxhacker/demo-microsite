# see: https://www.elastic.co/guide/en/logstash/current/docker-config.html
FROM docker.elastic.co/logstash/logstash-oss:8.8.2

RUN rm -f /usr/share/logstash/pipeline/logstash.conf

WORKDIR /usr/share/logstash
RUN ./bin/logstash-plugin install logstash-output-loki

COPY config/ /usr/share/logstash/config/

