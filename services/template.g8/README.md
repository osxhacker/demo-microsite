Microservice Giter8 Template
============================

This directory provides a [Giter8](http://www.foundweekends.org/giter8/) template for a minimal Scala microservice definition.

For example, the following could be executed to create a microservice named "hello world":

```
cd $(git rev-parse --show-toplevel)/services &&
	g8 "file://$PWD/template.g8/" --name="Hello World"
```

This would create a directory named `hello-world` having Scala `package`s under `com.github.osxhacker.demo.helloWorld`.  The structure would approximate:

```
hello-world
└── src
    ├── main
    │   ├── resources
    │   └── scala
    │       └── com
    │           └── github
    │               └── osxhacker
    │                   └── demo
    │                       └── helloWorld
    │                           ├── adapter
    │                           │   └── rest
    │                           │       └── arrow
    │                           └── domain
    └── test
        ├── resources
        └── scala
            └── com
                └── github
                    └── osxhacker
                        └── demo
                            └── helloWorld
                                ├── adapter
                                └── domain
```

