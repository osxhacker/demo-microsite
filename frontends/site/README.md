Site
====

This sub-project defines the `site` microsite.  It is the entry point for browser-based system use.  Details as to how to deploy it can be found in the [deployment](../../deployment/README.md) documentation.

A hypothesis being explored with this microsite is:

> Can a browser-based front end be implemented using no custom application logic?  In this context, "custom logic" is defined as code written in Scala or JavaScript specific to the application.

The implementation here attempts to answer in the affirmative.


## Apache Camel

[Apache Camel](https://camel.apache.org/docs/index.html) is used to provide pre-defined components which serve and respond to HTML5 and CSS3 artifacts.  They are configured using [XML DSL](https://camel.apache.org/components/3.20.x/others/java-xml-io-dsl.html) located in the [camel resources](./src/main/resources/camel) directory.


## HTMX

Responsive HTML5 content is provided by the [htmx](https://htmx.org/) library.


