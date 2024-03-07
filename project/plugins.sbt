libraryDependencies ++= Seq (
	"com.commercetools.rmf" % "raml-model" % "0.2.0-20221013103753"
	)

addDependencyTreePlugin

/// see: https://github.com/commercetools/scraml
addSbtPlugin ("com.commercetools" % "sbt-scraml" % "0.13.1")

/// see: https://github.com/marcuslonnberg/sbt-docker
addSbtPlugin ("se.marcuslonnberg" % "sbt-docker" % "1.11.0")

/// see: https://gatling.io/docs/gatling/reference/current/extensions/sbt_plugin/
addSbtPlugin ("io.gatling" % "gatling-sbt" % "4.6.0")

/// see: https://github.com/sbt/sbt-native-packager
addSbtPlugin ("com.github.sbt" % "sbt-native-packager" % "1.9.16")

/// see: https://github.com/djspiewak/sbt-github-packages
addSbtPlugin("com.codecommit" % "sbt-github-packages" % "0.5.3")

