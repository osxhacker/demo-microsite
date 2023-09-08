Service Logging
===============


Each deployed project service *must* configure and make available standardized logging functionality.  Third-party services which support configurable logging such that they can produce JSON log events *should be* configured to produce them.

The purpose of this document is to define the expectations of supported logging frameworks and how noteworthy features should be used.

It is expected that the reader is familiar with [The 10 commandments of logging](https://www.masterzen.fr/2013/01/13/the-10-commandments-of-logging/).  While it contains excellent recommendations, this document may vary from it where applicable.


## General

Logging *must not* be used for performance metrics, with the one exception of reporting individual "slow operations."  Since metrics collection is allowed to aggregate, there is no guarantee an individual workflow can be represented by it.  Therefore, logging *can be* used in this situation only.


### Log Levels

The `ERROR` logging level is reserved for reporting conditions which are not recoverable within a given invocation.  For example, if a `REST` resource is provided which does not pass validation.  Another example would be if there were a network error when interacting with a supporting service (such as a RDBMS).

Use `WARN` for situations where there is likely a recoverable failure *or* when the service detects a condition which may degrade into an `ERROR`.  An example of a recoverable failure is if an entity could not be found which is managed by eventual consistency workflows.

There are three categories where an `INFO` log event is expected to be produced:

1. Created an aggregate root.
2. Deleted an aggregate root.
3. A change which has a significant cascading affect.

While the first two categories have objective applicability, the third is subjective.  In this context, "a significant cascading affect" is defined as an operation which can cause dependent entities to be mutated.

For example, when a `Company` is deleted by the `company-service`, an event is emitted for all interested services to consume.  One of these is the `storage-facility-service`, which will respond to this event by attempting to delete all `StorageFacility` instances "owned" by the deleted `Company`.

The `DEBUG` logging level *must* be used only for information relevant to a developer running services in a local environment.  Further, `DEBUG` log events *must not* be emitted to log aggregators visible to any environment other than the one a specific developer is using.

The `TRACE` log level is not currently used.


## Elastic Common Schema (ECS)

The JSON log event format supported is [ECS in logstash](https://www.elastic.co/guide/en/logstash/current/ecs-ls.html).  A minimal example produced by the `company` microservice when requested to shut down is:

```json
{
	"@timestamp" : "2023-09-04T17:22:53.874044-07:00",
	"@version" : "1",
	"message" : "initiating shutdown isOnline=true",
	"logger_name" : "com.github.osxhacker.demo.company.adapter.rest.Shutdown",
	"thread_name" : "io-compute-3",
	"level" : "INFO",
	"level_value" : 20000,
	"HOSTNAME" : "<fqdn>",
	"subsystem" : "rest",
	"correlationId" : "a41941b3-fba1-4b02-8f3c-0dc0107dc573",
	"url.path" : "/api/internal/shutdown",
	"region" : "local",
	"forwarded" : "N/A",
	"user-agent" : "curl/8.2.1",
	"host.name" : "<fqdn>",
	"service.name" : "company"
}
```

Notes:

* The JSON object is usually "flat".  ECS does allow some fields to be arrays or simple objects.
* `<fqdn>` is the fully qualified domain name for the machine running the service.
* `correlationId` is expected to be propagated across service interactions.


## Subsystem Minimum Context

Each microservice subsystem is expected to establish a "minimum context" consistent with what is *always* available in each.  Additional context can and often will exist.

### Shared

*All* subsystems *must* have at least the following minimal context:

* The `region` where the machine is deployed.
* The `host.name` of the machine.
* The `service.name` deployed.
* The `subsystem` identifying what specific subsystem is executing.
* The `correlationId` specific to each request.


### REST

REST controllers *must* create a minimum context containing:

* What `url.path` received the request.


### Kafka

Kafka event processors *must* have at least the following minimal context:

* What `channel` is being processed.
* The `operation` being performed.


## Log4Cats

The [log4cats](https://typelevel.org/log4cats/) library is used in Scala-based microservices.  There are only a couple of notes to its use included here.

First, [log4cats](https://typelevel.org/log4cats/) defers evaluation within a container `F[_]`.  This implies log statements *must* be included by chaining production of `F[_]` instances.  This is opposite of many log frameworks, as they rely on immediate effectual evaluation.

Second, the project-specific `ContextualLoggerFactory` type provides the ability to establish a minimal context associated with *each* [log4cats](https://typelevel.org/log4cats/) `org.typelevel.log4cats.SelfAwareStructuredLogger` created by it.  By doing so, collaborators such as `correlationId` and `region` are guaranteed to be present througout.


## Logback

The [logback](https://logback.qos.ch/documentation.html) library was chosen as the underlying logging library.  Key reasons as to why it was selected are:

* Ability to emit log events asynchronously.
* Able to produce [logstash](https://www.elastic.co/guide/en/logstash/current/introduction.html)-compatible log events.
* Supported by SLF4j and `java.util.logging` (via SLF4j if needed).

Others, such as Log4j2, likely could be used in its place if needed so long as they satisfy the above function points.


