# 6. system security

Date: 2023-08-07


## Status

Accepted


## Context

Securing systems, be they monoliths, microservices, or other architectures, is a key concern.  What choices are made to secure them require careful thought and should be revisited during the lifetime of a product.

There are numerous technologies and/or techniques applicable in helping to secure an offering, such as:

* SSL/TLS certificate managers
* Key rotation
* Multifactor authentication
* Oauth2/OpenID Connect
* Network isolation
* Principle of least privilege
* Data encryption at rest


## Decision

Since the intent of this project is to be an exploration of implementation and operational concerns, securing the demo-microsite services is not addressed *when running locally in Docker*.

Once deployment in a cloud offering is supported, some or all of the concepts enumerated above *must* be employed at that time.


## Consequences

Security concepts will not be addressed until non-local deployment is supported.  This may increase difficulty by having to retrofit security security concepts into existing services.

