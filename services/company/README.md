Company Microservice
====================

This sub-project defines the `copany` microservice.  Details as to how to deploy it can be found in the [deployment](../../deployment/README.md) documentation.


```mermaid
---
title: Key Abstractions
---
classDiagram

Company *-- Identifier : id
Company *-- Version : version
Company *-- CompanyStatus : status
Company o-- "0..1" ModificationTimes : modificationTimes

class Company {
    +String name
    +String description

    +changeStatusTo(candidate) ErrorOr
}

class CompanyStatus {
	+String entryName

	+canBecome(candidate) Boolean
}

class ErrorOr

class Identifier {
    +String nid
    +String nss
}

class ModificationTimes {
    +datetime createdOn
    +datetime lastChanged

    +touch() ModificationTimes
}

class Version {
    +positive value
}
```

```mermaid
---
title: Allowable Status Transitions
---
stateDiagram-v2
	Inactive --> Active
	Active --> Inactive
	Active --> Suspended
	Suspended --> Active
```


## Service Notes

### Kafka as a Persistent Store

The `company-service` differs from others in that it uses Kafka as its persistent store between invocations.  This is done both as an experiment to discover the implications of doing so as well as serving to cauterize dependencies.


