Company
=======

This sub-project defines the `company` microsite.  It is for browser-based `Company` administration.  Details as to how to deploy it can be found in the [deployment](../../deployment/README.md) documentation.

A hypothesis being explored with this microsite is:

> Can a browser-based front end be implemented using no custom application logic?  In this context, "custom logic" is defined as code written in Scala or JavaScript specific to the application.

The implementation here attempts to answer in the affirmative.


## Published Entry Points

All `company-site` [URI's](https://datatracker.ietf.org/doc/html/rfc3986/) considered "public", which can be defined as both stable and published, can be found in the [company-site RAML](../../api/src/main/raml/microsite/company.raml).

Any other [URI](https://datatracker.ietf.org/doc/html/rfc3986/) **must** be considered transient.


## Services Used

* [Company Service](../../services/company/README.md)


## Apache Camel

[Apache Camel](https://camel.apache.org/docs/index.html) is used to provide pre-defined components which serve and respond to HTML5 and CSS3 artifacts.  They are configured using [XML DSL](https://camel.apache.org/components/3.20.x/others/java-xml-io-dsl.html) located in the [camel resources](./src/main/resources/camel) directory.

The [REST endpoints](./src/main/resources/camel/rest/endpoints.xml) define the API which serves content and responds to browser commands.


## HTMX

Responsive HTML5 content is provided by the [htmx](https://htmx.org/) library.


## Velocity

Dynamic HTML5 content is produced by the microsite using [Apache Camel](https://camel.apache.org/docs/index.html) [Velocity](https://velocity.apache.org/engine/devel/user-guide.html) component.


