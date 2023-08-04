# 3. Implement using microservices

Date: 2022-09-13

## Status

Accepted

## Context

The purpose of this demonstration project is to define a "microsite", which can be defined as a focused web application which is loosely coupled with how the application's functionality is provided.  While the problem domain is intentionally kept simple, it is sufficiently intricate to allow exposition of interesting system architecture.

## Decision

A [microservices](https://microservices.io/index.html) implementation architecture is chosen so that the following can be supported as needed:

- Command Query Responsibility Segregation (CQRS)
- Event Sourcing
- Eventual Consistency
- Service-Specific Persistence
- Service Orchestration

Each will be decided upon in subsequent ADR's.

## Consequences

Using a [microservices](https://microservices.io/index.html) architecture allows for easier enhancements within a bounded context.  It also provides a path for service-specific dynamic scaling/failure recovery.  Finally, degraded system functionality can be supported in the presence of a subset of services being unavailable.

However, this type of architecture can obfuscate service dependencies if not managed carefully.  Key to minimizing this risk is having a solution for "distributed tracing."

