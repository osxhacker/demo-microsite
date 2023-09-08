# 7. Logging Philosophy

Date: 2023-08-25

## Status

Accepted

## Context

Every system has a philosophy which defines how activity logging is performed, even if the philosophy is to not log activity at all or, more commonly, emit logs ad hoc.

As such, how activity logging is performed by the services which constitute this demonstration project *must* be clearly defined so that there exists consistency, predictable content and format, and perhaps just as importantly what is explicitly not logged.

It is expected that the reader is familiar with [The 10 commandments of logging](https://www.masterzen.fr/2013/01/13/the-10-commandments-of-logging/).  While it contains excellent recommendations, this ADR may vary from it where applicable.


## Decision

A key architectural decision which is not evident elsewhere is that "success logging" *must not* be performed.  Here, "success logging" is defined as emitting log events for operations which peform as expected.  It is expected that unit, feature, integration, and [black-box](https://en.wikipedia.org/wiki/Black-box_testing) testing verify system logic for fitness of purpose.

Each service *must* use an existing logging library component in its implementation.  Specifically, there is no justifiable reason for "rolling your own" logging framework.

For Scala-based services, the [log4cats](https://typelevel.org/log4cats/) project is recommended.  With its `log4cats-slf4j` support, it is easily able to use the recommended [logback](https://logback.qos.ch/documentation.html) JVM logging framework.

Log configurations *must* support producing [logstash](https://www.elastic.co/guide/en/logstash/current/introduction.html) compatible JSON events.  "Plain" logging *can be* supported for local development purposes.

`DEBUG` and `TRACE` log events *must not* be required for successful operations monitoring.  Additional recommendations can be found in the project-specific logging documentation.


## Consequences

Having standardized logging should make monitoring easier to define and establish expectations for what is and, more importantly, what *is not* included in log events.

