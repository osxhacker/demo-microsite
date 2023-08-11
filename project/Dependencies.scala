package com.github.osxhacker.demo

import sbt._


/**
 * The '''Dependencies''' `object` defines __all__ available build libraries.
 */
object Dependencies
{
	/**
	 * The '''Versions''' `object` defines the unique version ''String''s
	 * needed to resolve dependencies.
	 */
	object Versions
	{
		/// Per-project Compiler Plugins
		val BetterMonadicFor = "0.3.1"
		val KindProjector = "0.13.2"

		/// Main Artifacts
		val Camel = "3.20.4"
		val Cats = "2.9.0"
		val CatsEffect = "3.4.8"
		val CatsEffectTesting = "1.5.0"
		val Chimney = "0.6.2"
		val Circe = "0.14.5"
		val Decline = "2.4.1"
		val Diffx = "0.8.2"
		val Doobie = "1.0.0-RC2"
		val Enumeratum = "1.7.2"
		val Fs2 = "3.6.1"
		val Fs2Kafka = "3.0.1"
		val Gatling = "3.9.5"
		val Htmx = "1.9.2"
		val Http4sBlaze = "0.23.14"
		val Janino = "3.1.10"
		val JQuery = "3.7.0"
		val Kamon = "2.6.3"
		val Log4Cats = "2.6.0"
		val Logback = "1.4.6"
		val LogbackContrib = "0.1.5"
		val LogstashLogbackEncoder ="7.4"
		val Monocle = "3.2.0"
		val Mouse = "1.2.1"
		val Parsley = "2.9.2"
		val Pureconfig = "0.17.4"
		val PureCSS = "3.0.0"
		val Refined = "0.10.3"
		val Shapeless = "2.3.10"
		val Squants = "1.8.3"
		val Sttp ="3.8.8"
		val Tapir = "1.4.0"

		/// Testing Artifacts
		val Scalacheck = "1.17.0"
		val ScalacheckShapeless = "1.3.1"
		val Scalatest = "3.2.15"
		val ScalatestScalacheck = "3.2.14.0"
	}


	/// Per-project Compiler Plugins
	val BetterMonadicFor = "com.olegpy" %% "better-monadic-for" % Versions.BetterMonadicFor
	val KindProjector = "org.typelevel" % "kind-projector" % Versions.KindProjector cross CrossVersion.full

	/// Main Artifacts
	val CamelBean = "org.apache.camel" % "camel-bean" % Versions.Camel
	val CamelCore = "org.apache.camel" % "camel-core" % Versions.Camel
	val CamelHttp = "org.apache.camel" % "camel-http" % Versions.Camel
	val CamelJackson = "org.apache.camel" % "camel-jackson" % Versions.Camel
	val CamelJetty = "org.apache.camel" % "camel-jetty" % Versions.Camel
	val CamelJslt = "org.apache.camel" % "camel-jslt" % Versions.Camel
	val CamelLog = "org.apache.camel" % "camel-log" % Versions.Camel
	val CamelMain = "org.apache.camel" % "camel-main" % Versions.Camel
	val CamelNettyHttp = "org.apache.camel" % "camel-netty-http" % Versions.Camel
	val CamelOgnl = "org.apache.camel" % "camel-ognl" % Versions.Camel
	val CamelRest = "org.apache.camel" % "camel-rest" % Versions.Camel
	val CamelXmlDsl = "org.apache.camel" % "camel-xml-io-dsl" % Versions.Camel
	val CamelVelocity = "org.apache.camel" % "camel-velocity" % Versions.Camel
	val Cats = "org.typelevel" %% "cats-core" % Versions.Cats
	val CatsEffect = "org.typelevel" %% "cats-effect" % Versions.CatsEffect
	val Chimney = "io.scalaland" %% "chimney" % Versions.Chimney
	val ChimneyCats = "io.scalaland" %% "chimney-cats" % Versions.Chimney
	val CirceCore = "io.circe" %% "circe-core" % Versions.Circe
	val CirceGeneric = "io.circe" %% "circe-generic" % Versions.Circe
	val CirceParser = "io.circe" %% "circe-parser" % Versions.Circe
	val CirceRefined = "io.circe" %%
		"circe-refined" %
		Versions.Circe excludeAll (
			ExclusionRule (organization = "eu.refined"),
			ExclusionRule (organization = "org.scala-lang.modules")
			)

	val Decline = "com.monovore" %% "decline" % Versions.Decline
	val DeclineEffect = "com.monovore" %% "decline-effect" % Versions.Decline
	val DeclineRefined = "com.monovore" %% "decline-refined" % Versions.Decline
	val DiffxCats = "com.softwaremill.diffx" %% "diffx-cats" % Versions.Diffx
	val DiffxCore = "com.softwaremill.diffx" %% "diffx-core" % Versions.Diffx
	val DiffxRefined = "com.softwaremill.diffx" %% "diffx-refined" % Versions.Diffx
	val DoobieCore = "org.tpolecat" %% "doobie-core" % Versions.Doobie
	val DoobieHikari = "org.tpolecat" %% "doobie-hikari" % Versions.Doobie
	val DoobiePostgres = "org.tpolecat" %% "doobie-postgres" % Versions.Doobie
	val DoobiePostgresCirce = "org.tpolecat" %% "doobie-postgres-circe" % Versions.Doobie
	val DoobieRefined = "org.tpolecat" %% "doobie-refined" % Versions.Doobie
	val Enumeratum = "com.beachape" %% "enumeratum" % Versions.Enumeratum
	val EnumeratumCats = "com.beachape" %% "enumeratum-cats" % Versions.Enumeratum
	val Fs2Core = "co.fs2" %% "fs2-core" % Versions.Fs2
	val Fs2Io = "co.fs2" %% "fs2-io" % Versions.Fs2
	val Fs2Kafka = "com.github.fd4s" %%
		"fs2-kafka" %
		Versions.Fs2Kafka excludeAll (
			ExclusionRule (organization = "co.fs2")
			)

	val Htmx = "org.webjars.npm" % "htmx.org" % Versions.Htmx
	val Http4sBlaze = "org.http4s" %% "http4s-blaze-server" % Versions.Http4sBlaze
	val JacksonDatabind = "com.fasterxml.jackson.core" %
		"jackson-databind" %
		"2.14.2" %
		Runtime

	val Janino = "org.codehaus.janino" % "janino" % Versions.Janino
	val JQuery = "org.webjars" % "jquery" % Versions.JQuery
	val KamonBundle = "io.kamon" %% "kamon-bundle" % Versions.Kamon
	val KamonCatsIo = "io.kamon" %% "kamon-cats-io-3" % Versions.Kamon
	val KamonJdbc = "io.kamon" %% "kamon-jdbc" % Versions.Kamon
	val KamonPrometheus = "io.kamon" %% "kamon-prometheus" % Versions.Kamon
	val KamonSystemMetrics = "io.kamon" %% "kamon-system-metrics" % Versions.Kamon
	val Log4Cats = "org.typelevel" %% "log4cats-slf4j" % Versions.Log4Cats
	val Logback = "ch.qos.logback" % "logback-classic" % Versions.Logback % Runtime
	val LogbackJackson = "ch.qos.logback.contrib" %
		"logback-jackson" %
		Versions.LogbackContrib %
		Runtime

	val LogbackJson = "ch.qos.logback.contrib" %
		"logback-json-classic" %
		Versions.LogbackContrib %
		Runtime

	val LogstashLogbackEncoder = "net.logstash.logback" %
		"logstash-logback-encoder" %
		Versions.LogstashLogbackEncoder %
		Runtime

	val MonocleCore = "dev.optics" %% "monocle-core" % Versions.Monocle
	val MonocleGeneric = "dev.optics" %% "monocle-generic" % Versions.Monocle
	val MonocleMacro = "dev.optics" %% "monocle-macro" % Versions.Monocle
	val Mouse = "org.typelevel" %% "mouse" % Versions.Mouse
	val Parsley = "org.webjars.npm" % "parsleyjs" % Versions.Parsley
	val Pureconfig = "com.github.pureconfig" %% "pureconfig" % Versions.Pureconfig
	val PureconfigCats = "com.github.pureconfig" %% "pureconfig-cats" % Versions.Pureconfig
	val PureCSS = "org.webjars.npm" % "purecss" % Versions.PureCSS
	val Refined = "eu.timepit" %% "refined" % Versions.Refined
	val RefinedCats = "eu.timepit" %% "refined-cats" % Versions.Refined
	val RefinedPureconfig = "eu.timepit" %% "refined-pureconfig" % Versions.Refined
	val RefinedShapeless = "eu.timepit" %% "refined-shapeless" % Versions.Refined excludeAll (
		ExclusionRule (organization = "com.chuusai")
		)

	val Squants = "org.typelevel" %% "squants" % Versions.Squants
	val Shapeless = "com.chuusai" %% "shapeless" % Versions.Shapeless
	val TapirCore = "com.softwaremill.sttp.tapir" %% "tapir-core" % Versions.Tapir
	val TapirHttp4s = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % Versions.Tapir
	val TapirJsonCirce = "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.Tapir

	/// Testing Artifacts
	val CatsEffectTesting = "org.typelevel" %%
		"cats-effect-testing-scalatest" %
		Versions.CatsEffectTesting excludeAll (
			ExclusionRule (organization = "org.scalatest")
			)

	val DoobieScalatest = "org.tpolecat" %% "doobie-scalatest" % Versions.Doobie
	val GatlingCore = "io.gatling" % "gatling-test-framework" % Versions.Gatling
	val GatlingCharts = "io.gatling.highcharts" %
		"gatling-charts-highcharts" %
		Versions.Gatling

	val KamonTestkit = "io.kamon" %% "kamon-testkit" % Versions.Kamon
	val Log4CatsNoop = "org.typelevel" %% "log4cats-noop" % Versions.Log4Cats
	val RefinedScalacheck = "eu.timepit" %%
		"refined-scalacheck" %
		Versions.Refined excludeAll (
			ExclusionRule (organization = "org.scalacheck")
			)

	val Scalacheck = "org.scalacheck" %% "scalacheck" % Versions.Scalacheck
	val ScalacheckShapeless = "com.github.alexarchambault" %%
		"scalacheck-shapeless_1.16" %
		Versions.ScalacheckShapeless

	val Scalatest = "org.scalatest" %% "scalatest" % Versions.Scalatest
	val ScalatestScalacheck = "org.scalatestplus" %%
		"scalacheck-1-16" %
		Versions.ScalatestScalacheck

	val SttpClientCirce = "com.softwaremill.sttp.client3" %%
		"circe" %
		Versions.Sttp

	val TapirClient = "com.softwaremill.sttp.tapir" %%
		"tapir-sttp-client" %
		Versions.Tapir

	val TapirStubServer = "com.softwaremill.sttp.tapir" %%
		"tapir-sttp-stub-server" %
		Versions.Tapir
}

