libraryDependencies ++= Seq (
	"com.commercetools.rmf" % "raml-model" % "0.2.0-20240722205528"
	)

addDependencyTreePlugin

/// see: https://github.com/commercetools/scraml
addSbtPlugin ("com.commercetools" % "sbt-scraml" % "0.18.0")

/// see: https://www.scala-sbt.org/sbt-native-packager/index.html
addSbtPlugin ("com.github.sbt" % "sbt-native-packager" % "1.11.7")

/// see: https://gatling.io/docs/gatling/reference/current/extensions/sbt_plugin/
addSbtPlugin ("io.gatling" % "gatling-sbt" % "4.6.0")

/// see: https://github.com/sbt/sbt-github-actions
addSbtPlugin ("com.github.sbt" % "sbt-github-actions" % "0.25.0")

