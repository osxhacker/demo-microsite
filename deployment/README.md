Deployment
==========

This directory contains the configuration assets required to deploy the demo microsite services in supported environments.


## Docker Compose

[Docker Compose](https://docs.docker.com/compose/) is supported primarily for local development use.  While the images used and created could be deployed in hosted environments, they support building locally in order to enable easy integration testing.

More information can be found [here](./docker/README.md).

The following table documents what `localhost` IP ports are exposed for each service when all are running.

| Service          |  API      |  Kamon  |  Prometheus  |  Admin  |
|------------------|:---------:|:-------:|:------------:|:-------:|
| API Gateway      |  9080     |         |              |  9180   |
| Web Site         |  11610    |         |              |         |
| Company Site     |  11611    |         |              |         |
| Facility Site    |  11612    |         |              |         |
| Company Service  |  6891     |  5238   |  9557        |         |
| Facility Service |  6890     |  5237   |  9556        |         |
| Logstash         |  5514/udp |         |              |  9600   |
| Postgres         |  6430     |         |              |         |
| Mongo            |  6440     |         |              |         |
| Kafka            |  9092     |         |              |  9999   |
| Zookeeper        |  2181     |         |              |         |
| Loki             |  3100     |         |              |         |
| Prometheus       |  9090     |         |              |         |
| Grafana          |  3000     |         |              |         |


## Azure

*TBD*

