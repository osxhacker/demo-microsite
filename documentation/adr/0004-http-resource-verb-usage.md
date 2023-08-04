# 4. http resource verb usage

Date: 2022-10-17

## Status

Accepted

## Context

HTTP-based RESTful resources have options as to how a subset of supported verbs are used.  Specifically, the expectations of [idempotent verbs vs those which are not](https://restcookbook.com/HTTP%20Methods/idempotency/) and how they are supported in the system.

For consistency both in implementation and expectations, the rules for each supported HTTP verb are defined herein.

In the context of this ADR, a "versioned resource" is one which is either an aggregate root or an entity, whose identity is determined outside of its individual properties.  A "collection" is a resource containing zero or more homogeneous resources.  A "value object" is an unversioned resource having a cardinality of one.  Finally, a "collection item" is a resource which exists within a collection and may be either a "versioned resource" or a "value object."

## Decision

There are five HTTP verbs potentially supported by resource endpoints; DELETE, GET, PATCH, POST, and PUT.  The expectations of each and when they may be supported are detailed below.  Whether or not a _specific_ resource supports each is up to it.

### DELETE

The DELETE verb is idempotent and unsafe.

- Versioned resource: supported
- Collection: not supported
- Collection item: supported
- Value object: supported

### GET

The GET verb is idempotent and safe.

- Versioned resource: supported
- Collection: supported
- Collection item: supported
- Value object: supported

### PATCH

The PATCH verb is not idempotent and unsafe.

- Versioned resource: supported [0]
- Collection: not supported
- Collection item: supported [1]
- Value object: not supported

0 - Requires the `id` and `version` to be specified and may result in a "stale object" error if `version` is not the latest one.

1 - Supported if the item is a versioned resource.

### POST

The POST verb is not idempotent and unsafe.

- Versioned resource: supported [0]
- Collection: supported [1]
- Collection item: supported
- Value object: not supported

0 - Versioned resources **must** support modification via POST since both alter the version and modification times with _each_ invocation.

1 - A collection which supports POST of its items **must** specify `uniqueItems: false` in its RAML contract.

### PUT

The PUT verb is idempotent and unsafe.

- Versioned resource: not supported [0]
- Collection: supported [1]
- Collection item: supported [2]
- Value object: supported

0 - Altering a versioned resource **must** be done via POST.

1 - If supported for versioned resources, **only** "new" resources can be accepted (see Consequences below).

2 - PUT of an item within a collection of value object resources **must** create a new item if a value-matching entry does not exist and update if it does.

## Consequences

Of the supported HTTP verbs, POST and PUT have contextual semantics based on whether or not what is being manipulated exists when the request is submitted.  This implies the server must be able to determine existence based strictly on the payload and existing persistent state.

New versioned resources **must not** have `id`, `version`, or modification time properties.  Existing versioned resources **must** have at least `id` and `version`, and **may** have modification times, though the RESTful endpoint is encouraged to not use submitted modification times.

Each collection **must** define whether or not adding items is idempotent.  The decision matrix used to make the determination is:

- Are collection items unique?
- Does uniqueness exist independent of service state?
- Does the client provide the uniqueness?


If the answers to all of those questions are "yes", then PUT should be the verb used to add items.  This implies the services **must** handle duplicate PUT invocations in order to satisfy the idempotent contract.

If any of the above answers is "no", the POST should be the verb used to add items.

While it is possible to support both POST and PUT based on the new resource being provided, this is not recommended and should only be introduced if absolutely necessary.


