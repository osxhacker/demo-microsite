Gatling Simulations
===================

This sub-project defines the [Gatling](https://gatling.io/docs/gatling/tutorials/installation/) feature and load-testing micro-service [simulations](https://gatling.io/docs/gatling/reference/current/core/simulation/).  Only the [RAML API](../api/src/main/raml) contracts are shared between this project and the [services](../services).

Each [simulation](https://gatling.io/docs/gatling/reference/current/core/simulation/) defaults to interacting with services running on `localhost` (as defined in service entrypoint RAML's [here](../api/src/main/raml)).  To have a [simulation](https://gatling.io/docs/gatling/reference/current/core/simulation/) interact with services deployed elsewhere, the following JVM system properties can be used:

- **service.endpoint**: This defines the "primary" service under test.
- **service.<symbolic name>**: These define locations for individual supporting services.

For example, to run `StorageFacilityFeatures` using the micro-service deployed at `my.cool.cloud` (port `9999`) and the company service running locally, one would run:

```
cd $(git rev-parse --show-toplevel) && sbt \
	-Dservice.endpoint=http://my.cool.cloud:9999 \
	'gatling / GatlingIt / testOnly *StorageFacilityFeatures'
```

To run the same [simulation](https://gatling.io/docs/gatling/reference/current/core/simulation/) with both services deployed to `my.cool.cloud`, one would run:

```
cd $(git rev-parse --show-toplevel) && sbt \
	-Dservice.endpoint=http://my.cool.cloud:9999 \
	-Dservice.company=http://my.cool.cloud:9998 \
	'gatling / GatlingIt / testOnly *StorageFacilityFeatures'
```


## Prerequisites

In order for the [simulations](https://gatling.io/docs/gatling/reference/current/core/simulation/) to operate *locally*, the following must be running and available:

* [Repositories](../deployment/docker/repositories/docker-compose.yml)
* [Services](../deployment/docker/services/docker-compose.yml)

Optionally, the following can be running:

* [Frontends](../deployment/docker/frontends/docker-compose.yml)
* [Operations](../deployment/docker/operations/docker-compose.yml)


Those same instances identified in the [Docker Compose definitions](../deployment/docker) must be available and configured appropriately in a cloud environment in order to run *remote* [simulations](https://gatling.io/docs/gatling/reference/current/core/simulation/).


## Feature Simulations

To run all feature [simulations](https://gatling.io/docs/gatling/reference/current/core/simulation/), execute:

```
cd $(git rev-parse --show-toplevel) && sbt 'run-feature-simulations'
```


## Load Test Simulations

Each load test is designed to be executed individually.  This is due to the expected stress they are intended to induce, which would make measuring the system difficult to do if more than one load test were executing at the same time.

There are several knobs available to each load test, documented [here](./src/main/scala/com/github/osxhacker/demo/gatling/LoadTestSimulation.scala).  Each load test is free to use some, none, or all available `settings`.

[StorageFacilityLoadTest](src/it/scala/com/github/osxhacker/demo/gatling/service/storageFacility/StorageFacilityLoadTest.scala) is an example of a load test.  Below are sample invocations to illustrate its use:


### Burst Usage (Direct)

```
cd $(git rev-parse --show-toplevel) && ./scripts/run-load-test.sh \
	StorageFacilityLoadTest \
	LOG_LEVEL=error \
	simulation.delay=5 \
	simulation.burstUsers=100
```


### Burst And Ramp Usage (Direct)

```
cd $(git rev-parse --show-toplevel) && ./scripts/run-load-test.sh \
	StorageFacilityLoadTest \
	LOG_LEVEL=error \
	simulation.delay=5 \
	simulation.burstUsers=100 \
	simulation.rampUsers=50 \
	simulation.rampUsersWindow=10
```


### Constant Usage (Direct)

```
cd $(git rev-parse --show-toplevel) && ./scripts/run-load-test.sh \
	StorageFacilityLoadTest \
	LOG_LEVEL=error \
	simulation.constantUsers=50 \
	simulation.constantUsersWindow=10
```


### Everything (Direct)

```
cd $(git rev-parse --show-toplevel) && ./scripts/run-load-test.sh \
	StorageFacilityLoadTest \
	LOG_LEVEL=error \
	simulation.delay=5 \
	simulation.burstUsers=100 \
	simulation.constantUsers=50 \
	simulation.constantUsersWindow=10 \
	simulation.rampUsers=50 \
	simulation.rampUsersWindow=10
```


### Burst Usage (Gateway)

```
cd $(git rev-parse --show-toplevel) && ./scripts/run-load-test.sh \
	StorageFacilityLoadTest \
	LOG_LEVEL=error \
	service.endpoint=http://localhost:9080 \
	simulation.delay=5 \
	simulation.burstUsers=100
```


### Burst And Ramp Usage (Gateway)

```
cd $(git rev-parse --show-toplevel) && ./scripts/run-load-test.sh \
	StorageFacilityLoadTest \
	LOG_LEVEL=error \
	service.endpoint=http://localhost:9080 \
	simulation.delay=5 \
	simulation.burstUsers=100 \
	simulation.rampUsers=50 \
	simulation.rampUsersWindow=10
```


### Constant Usage (Gateway)

```
cd $(git rev-parse --show-toplevel) && ./scripts/run-load-test.sh \
	StorageFacilityLoadTest \
	LOG_LEVEL=error \
	service.endpoint=http://localhost:9080 \
	simulation.constantUsers=50 \
	simulation.constantUsersWindow=10
```


### Everything (Gateway)

```
cd $(git rev-parse --show-toplevel) && ./scripts/run-load-test.sh \
	StorageFacilityLoadTest \
	LOG_LEVEL=error \
	service.endpoint=http://localhost:9080 \
	simulation.delay=5 \
	simulation.burstUsers=100 \
	simulation.constantUsers=50 \
	simulation.constantUsersWindow=10 \
	simulation.rampUsers=50 \
	simulation.rampUsersWindow=10
```


