Storage Facility Microservice
=============================

This sub-project defines the `storage-facility` microservice.  Details as to how to deploy it can be found in the [deployment](../../deployment/README.md) documentation.


```mermaid
---
title: Key Abstractions
---
classDiagram

StorageFacility *-- Identifier : id
StorageFacility *-- Version : version
StorageFacility o-- "0..1" ModificationTimes : modificationTimes
StorageFacility *-- Volume : capacity
StorageFacility *-- Volume : available

class StorageFacility {
    +String name
    +String city
    +String state
    +String zip

    +changeStatusTo(candidate) ErrorOr
}

class ErrorOr

class ModificationTimes {
    +datetime createdOn
    +datetime lastChanged

    +touch() ModificationTimes
}

class Identifier {
    +String nid
    +String nss
}

class Version {
    +positive value
}

class Volume {
    +cubicmeters value

    +minus(volume) ErrorOr
    +plus(volume) Volume
}
```

```mermaid
---
title: Allowable Status Transitions
---
stateDiagram-v2
	Active --> Closed
	Closed --> Active
	UnderConstruction --> Active
```

