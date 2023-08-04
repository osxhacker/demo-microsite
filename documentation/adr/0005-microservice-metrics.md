# 5. Microservice Metrics

Date: 2023-01-20

## Status

Accepted

## Context

A critical concern when deploying microservices is being able to monitor run-time performance as well as easily identify abnormal workflow situations.  This is key in determining what provisioning decisions should be made as well as providing data for diagnosing problems.

System maintainability is a high priority in determining how metrics are captured.  Avoiding having to intertwine metrics usage with domain logic is highly desirable.

## Decision

Since metrics are a cross-cutting concern relevant to every workflow interacting with external agents defined by every microservice, a limited form of [Aspect Oriented Programming](https://en.wikipedia.org/wiki/Aspect-oriented_programming) approach has been selected.  Specifically, *categories* of metrics are solved for, with the ability to introduce logic tailored for a specific situation possible as well.

## Consequences

By taking an AOP approach, the type and names of metrics exposed has been normalized.  There may be some which may not be as useful in every context however.

Also, adding metrics at various points in a workflow is minimally intrusive.

