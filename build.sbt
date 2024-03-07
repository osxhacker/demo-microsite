import com.github.osxhacker.demo.GenerateSchemaDefinitions
import com.github.osxhacker.demo.Dependencies._


//////////////////////////////
/// Manifest Constants
//////////////////////////////

val systemName = "demo"


//////////////////////////////
/// Compiler Plugins
//////////////////////////////

addCompilerPlugin (BetterMonadicFor)
addCompilerPlugin (KindProjector)


//////////////////////////////
/// Command Aliases
//////////////////////////////

addCommandAlias (
	"compile-all",
	List (
		"compile",
		"Test / compile",
		"IntegrationTest / compile"
		).mkString (" ; ")
	)

addCommandAlias ("recompile", "clean ; compile")
addCommandAlias (
	"recompile-all",
	List (
		"clean",
		"compile",
		"Test / compile",
		"IntegrationTest / compile"
		).mkString (" ; ")
	)

addCommandAlias ("run-all-it", "IntegrationTest / test")
addCommandAlias (
	"run-feature-simulations",
	"gatling / GatlingIt / testOnly com.github.osxhacker.*Features"
	)


//////////////////////////////
/// Shared Settings
//////////////////////////////

ThisBuild / organization := s"com.github.osxhacker.$systemName"
ThisBuild / version := "0.6.4"

/// see: https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme
ThisBuild / versionScheme := Some ("semver-spec")

ThisBuild / scalaVersion := "2.13.13"
ThisBuild / Compile / scalacOptions ++= Seq (
	"-encoding", "UTF-8",
	"-release:9",
	"-deprecation",
	"-feature",
	"-unchecked",
	"-Wconf:cat=other-implicit-type:s",
	"-Xlog-reflective-calls",
	"-Xlint:-byname-implicit",
	"-Xlint:-doc-detached",
	"-Xlint:-unused,_",
	"-Ymacro-annotations",
	"-Ywarn-unused:-imports,_"
	)

ThisBuild / githubOwner := "osxhacker"
ThisBuild / githubRepository := "demo-microsite"


/// Ensure output is not as choppy
ThisBuild / Test / logBuffered := false

/// Set the prompt (for this build) to include the project id.
ThisBuild / shellPrompt := {
	state =>
		Project.extract (state).currentRef.project + "> "
	}


//////////////////////////////
/// Top-level Project
//////////////////////////////

lazy val microsite = (project in file ("."))
	.settings (publish / skip := true)
	.aggregate (api, frontends, services, gatling)


//////////////////////////////
/// API Project
//////////////////////////////

lazy val api = (project in file ("api"))
	.settings (
		name := "api",
		libraryDependencies ++=
			Seq (
				RefinedScalacheck,
				Scalacheck,
				ScalacheckShapeless,
				Scalatest,
				ScalatestScalacheck
				) ++
			Seq (
				Cats,
				CatsEffect,
				Chimney,
				CirceCore,
				CirceGeneric,
				CirceParser,
				CirceRefined,
				MonocleCore,
				Refined,
				RefinedCats,
				TapirCore,
				TapirJsonCirce
				).map (_ % Test),

		defaultTypes := scraml.DefaultTypes (
			array = "scala.collection.immutable.Vector",
			dateTime = "java.time.OffsetDateTime",
			float = "Double",
			double = "scala.math.BigDecimal",
			number = "scala.math.BigDecimal",
			time = "java.time.OffsetTime"
			),

		librarySupport := Set (
			scraml.libs.CatsEqSupport,
			scraml.libs.CatsShowSupport,
			scraml.libs.CirceJsonSupport (
				formats = Map (
					"offsetDateTime" -> "io.circe.Decoder.decodeOffsetDateTime",
					"offsetTime" -> "io.circe.Decoder.decodeOffsetTime"
					)
				),

			scraml.libs.MonocleOpticsSupport,
			scraml.libs.RefinedSupport,
			scraml.libs.TapirSupport ("Endpoints")
			),

		ramlDefinitions := Map (
			"inventory.raml" -> "inventory",
			"company.raml" -> "company",
			"purchase-order.raml" -> "purchaseOrder",
			"storage-facility.raml" -> "storageFacility"
			).map {
				case (ramlFile, packageName) =>

				scraml.ModelDefinition (
					raml = baseDirectory.value / s"src/main/raml/$ramlFile",
					basePackage = s"com.github.osxhacker.$systemName.api.$packageName",
					defaultPackageAnnotation = Some("APITypes"),
					fieldMatchPolicy = Some (scraml.FieldMatchPolicy.Exact ()),
					generateDateCreated = true
					)
				}
				.toSeq,

		/// The api project is has reference artifacts and, as such, only
		/// has Test-scoped Scala source code.
		Test / sourceGenerators += runScraml
		)


//////////////////////////////
/// Frontend Projects
//////////////////////////////

lazy val frontends = (project in file ("frontends"))
	.settings (publish / skip := true)
	.aggregate (
		`frontends-site`,
		`frontends-company`,
		`frontends-storage-facility`
		)

lazy val `frontends-company` = (project in file ("frontends/company"))
	.settings (
		name := "frontends-company",
		publish / skip := true,
		libraryDependencies ++= Seq (
			CamelBean,
			CamelCore,
			CamelHttp,
			CamelJackson,
			CamelJetty,
			CamelJslt,
			CamelLog,
			CamelMain,
			CamelOgnl,
			CamelRest,
			CamelXmlDsl,
			CamelVelocity,
			Htmx,
			JacksonDatabind,
			Janino,
			JQuery,
			Logback,
			LogbackJackson,
			LogbackJson,
			LogstashLogbackEncoder,
			Parsley,
			PureCSS
			),

		Compile / resourceGenerators += generateCamelMainProperties (
			"htmx" -> Versions.Htmx ::
			"jquery" -> Versions.JQuery ::
			"parsley" -> Versions.Parsley ::
			"purecess" -> Versions.PureCSS ::
			Nil
			).taskValue,

		docker / imageNames := Seq (
			ImageName (s"${organization.value}/${name.value}:latest")
			),

		docker / dockerfile := {
			val appDir = stage.value
			val installDir = "/app"
			val script = executableScriptName.value

			new Dockerfile {
				/// OpenJDK 19 is the base image.
				from ("eclipse-temurin:19-jre-focal")

				/// Allow traffic to the app ports.
				expose (12000)

				copy (appDir, installDir, chown = "daemon:daemon")

				/// Set the entrypoint to be invoking java with the app.
				entryPoint (
					s"$installDir/bin/$script"
					)
				}
			}
		)
	.dependsOn (services)
	.enablePlugins (sbtdocker.DockerPlugin, JavaAppPackaging)

lazy val `frontends-storage-facility` =
	(project in file ("frontends/storage-facility"))
	.settings (
		name := "frontends-storage-facility",
		publish / skip := true,
		libraryDependencies ++= Seq (
			CamelBean,
			CamelCore,
			CamelHttp,
			CamelJackson,
			CamelJetty,
			CamelJslt,
			CamelLog,
			CamelMain,
			CamelOgnl,
			CamelRest,
			CamelXmlDsl,
			CamelVelocity,
			Htmx,
			JacksonDatabind,
			Janino,
			JQuery,
			Logback,
			LogbackJackson,
			LogbackJson,
			LogstashLogbackEncoder,
			Parsley,
			PureCSS
			),

		Compile / resourceGenerators += generateCamelMainProperties (
			"htmx" -> Versions.Htmx ::
			"jquery" -> Versions.JQuery ::
			"parsley" -> Versions.Parsley ::
			"purecess" -> Versions.PureCSS ::
			Nil
			).taskValue,

		docker / imageNames := Seq (
			ImageName (s"${organization.value}/${name.value}:latest")
			),

		docker / dockerfile := {
			val appDir = stage.value
			val installDir = "/app"
			val script = executableScriptName.value

			new Dockerfile {
				/// OpenJDK 19 is the base image.
				from ("eclipse-temurin:19-jre-focal")

				/// Allow traffic to the app ports.
				expose (12000)

				copy (appDir, installDir, chown = "daemon:daemon")

				/// Set the entrypoint to be invoking java with the app.
				entryPoint (
					s"$installDir/bin/$script"
					)
				}
			}
		)
	.dependsOn (services)
	.enablePlugins (sbtdocker.DockerPlugin, JavaAppPackaging)

lazy val `frontends-site` = (project in file ("frontends/site"))
	.settings (
		name := "frontends-site",
		publish / skip := true,
		libraryDependencies ++= Seq (
			CamelBean,
			CamelCore,
			CamelHttp,
			CamelJetty,
			CamelLog,
			CamelMain,
			CamelOgnl,
			CamelRest,
			CamelXmlDsl,
			Htmx,
			JacksonDatabind,
			Logback,
			LogbackJackson,
			LogbackJson,
			PureCSS
			),

		Compile / resourceGenerators += generateCamelMainProperties (
			"htmx" -> Versions.Htmx ::
			"purecess" -> Versions.PureCSS ::
			Nil
			).taskValue,

		docker / imageNames := Seq (
			ImageName(s"${organization.value}/${name.value}:latest")
			),

		docker / dockerfile := {
			val appDir = stage.value
			val installDir = "/app"
			val script = executableScriptName.value

			new Dockerfile {
				/// OpenJDK 19 is the base image.
				from ("eclipse-temurin:19-jre-focal")

				/// Allow traffic to the app ports.
				expose (12000)

				copy (appDir, installDir, chown = "daemon:daemon")

				/// Set the entrypoint to be invoking java with the app.
				entryPoint (
					s"$installDir/bin/$script"
					)
				}
			}
		)
	.enablePlugins (sbtdocker.DockerPlugin, JavaAppPackaging)


//////////////////////////////
/// Service Projects
//////////////////////////////

lazy val services = (project in file ("services"))
	.settings (publish / skip := true)
	.aggregate (
		chassis,
		company,
		inventory,
		`purchase-order`,
		`storage-facility`
		)

lazy val chassis = (project in file ("services/chassis"))
	.settings (
		commonServiceSettings (),
		name := "services-chassis",
		libraryDependencies ++= Seq (
			/// Testing Artifacts
			ScalatestScalacheck
			),

		Test / parallelExecution := false,
		Test / publishArtifact := true
		)
	.dependsOn (api)

lazy val company = (project in file ("services/company"))
	.configs (IntegrationTest)
	.settings (
		Defaults.itSettings,
		commonServiceSettings ("it,test"),
		name := "services-company",
		publish / skip := true,
		ramlDefinitions := Seq (
			scraml.ModelDefinition (
				raml = file ("api/src/main/raml/company.raml"),
				defaultPackageAnnotation = Some ("APITypes"),
				basePackage = s"com.github.osxhacker.$systemName.company.adapter.rest.api",
				fieldMatchPolicy = Some (scraml.FieldMatchPolicy.Exact ()),
				defaultTypes = scraml.DefaultTypes (
					array = "scala.collection.immutable.Vector",
					dateTime = "java.time.OffsetDateTime",
					float = "Double",
					double = "scala.math.BigDecimal",
					number = "scala.math.BigDecimal",
					time = "java.time.OffsetTime"
					),

				librarySupport = Set (
					scraml.libs.CatsEqSupport,
					scraml.libs.CatsShowSupport,
					scraml.libs.CirceJsonSupport (
						formats = Map (
							"offsetDateTime" -> "io.circe.Decoder.decodeOffsetDateTime",
							"offsetTime" -> "io.circe.Decoder.decodeOffsetTime"
							)
						),

					scraml.libs.MonocleOpticsSupport,
					scraml.libs.RefinedSupport,
					scraml.libs.TapirSupport ("Endpoints")
					),

				generateDateCreated = true
				)
			),

		Compile / sourceGenerators += runScraml,
		IntegrationTest / parallelExecution := false,
		docker / imageNames := Seq (
			ImageName(s"${organization.value}/${name.value}:latest")
			),

		docker / dockerfile := {
			val appDir = stage.value
			val installDir = "/app"
			val script = executableScriptName.value

			new Dockerfile {
				/// OpenJDK 19 is the base image.
				from ("eclipse-temurin:19-jre-focal")

				/// Allow traffic to the app and administrative ports.
				expose (6891)

				copy (appDir, installDir, chown = "daemon:daemon")

				/// Set the entrypoint to be invoking java with the app.
				entryPoint (
					s"$installDir/bin/$script",
					"--",
					"--docker",
					"--json"
					)
				}
			}
		)
	.dependsOn (
		chassis % "compile->compile;test->test;it->test"
		)
	.enablePlugins (sbtdocker.DockerPlugin, JavaAppPackaging)

lazy val inventory = (project in file ("services/inventory"))
	.settings (
		name := "services-inventory",
		publish / skip := true,
		commonServiceSettings (),
		libraryDependencies ++= Seq (
			DoobieCore,
			DoobieHikari,
			DoobiePostgres,
			DoobiePostgresCirce,
			DoobieRefined,
			Squants,

			/// Testing Artifacts
			DoobieScalatest % Test
			),

		ramlDefinitions := Seq (
			scraml.ModelDefinition (
				raml = file ("api/src/main/raml/inventory.raml"),
				defaultPackageAnnotation = Some ("APITypes"),
				basePackage = s"com.github.osxhacker.$systemName.inventory.adapter.rest.api",
				fieldMatchPolicy = Some (scraml.FieldMatchPolicy.Exact ()),
				defaultTypes = scraml.DefaultTypes (
					array = "scala.collection.immutable.Vector",
					dateTime = "java.time.OffsetDateTime",
					float = "Double",
					double = "scala.math.BigDecimal",
					number = "scala.math.BigDecimal",
					time = "java.time.OffsetTime"
					),

				librarySupport = Set (
					scraml.libs.CatsEqSupport,
					scraml.libs.CatsShowSupport,
					scraml.libs.CirceJsonSupport (
						formats = Map (
							"offsetDateTime" -> "io.circe.Decoder.decodeOffsetDateTime",
							"offsetTime" -> "io.circe.Decoder.decodeOffsetTime"
							)
						),

					scraml.libs.MonocleOpticsSupport,
					scraml.libs.RefinedSupport,
					scraml.libs.TapirSupport ("Endpoints")
					),

				generateDateCreated = true
				)
			),

		Compile / sourceGenerators += runScraml,
		)
	.dependsOn (
		chassis % "compile->compile;test->test"
		)

lazy val `purchase-order` = (project in file ("services/purchase-order"))
	.settings (
		commonServiceSettings (),
		name := "services-purchase-order",
		publish / skip := true
		)
	.dependsOn (
		chassis % "compile->compile;test->test"
		)

lazy val `storage-facility` = (project in file ("services/storage-facility"))
	.configs (IntegrationTest)
	.settings (
		Defaults.itSettings,
		commonServiceSettings ("it,test"),
		name := "services-storage-facility",
		publish / skip := true,
		libraryDependencies ++= Seq (
			DoobieCore,
			DoobieHikari,
			DoobiePostgres,
			DoobiePostgresCirce,
			DoobieRefined,
			KamonJdbc,
			Squants,

			/// Testing Artifacts
			DoobieScalatest % "it,test"
			),

		ramlDefinitions := Seq (
			scraml.ModelDefinition (
				raml = file ("api/src/main/raml/storage-facility.raml"),
				defaultPackageAnnotation = Some ("APITypes"),
				basePackage = s"com.github.osxhacker.$systemName.storageFacility.adapter.rest.api",
				fieldMatchPolicy = Some (scraml.FieldMatchPolicy.Exact ()),
				defaultTypes = scraml.DefaultTypes (
					array = "scala.collection.immutable.Vector",
					dateTime = "java.time.OffsetDateTime",
					float = "Double",
					double = "scala.math.BigDecimal",
					number = "scala.math.BigDecimal",
					time = "java.time.OffsetTime"
					),

				librarySupport = Set (
					scraml.libs.CatsEqSupport,
					scraml.libs.CatsShowSupport,
					scraml.libs.CirceJsonSupport (
						formats = Map (
							"offsetDateTime" -> "io.circe.Decoder.decodeOffsetDateTime",
							"offsetTime" -> "io.circe.Decoder.decodeOffsetTime"
							)
						),

					scraml.libs.MonocleOpticsSupport,
					scraml.libs.RefinedSupport,
					scraml.libs.TapirSupport ("Endpoints")
					),

				generateDateCreated = true
				),

			scraml.ModelDefinition (
				raml = baseDirectory.value / s"src/main/raml/database.raml",
				basePackage =
					s"com.github.osxhacker.$systemName.storageFacility.adapter.database.schema",

				fieldMatchPolicy = Some (scraml.FieldMatchPolicy.Exact ()),
				defaultTypes = scraml.DefaultTypes (
					array = "scala.collection.immutable.Vector",
					dateTime = "java.time.Instant",
					float = "Double",
					double = "scala.math.BigDecimal",
					number = "scala.math.BigDecimal",
					time = "java.time.Instant"
					),

				librarySupport = Set (
					scraml.libs.CatsEqSupport,
					scraml.libs.CatsShowSupport,
					scraml.libs.RefinedSupport
					),

				generateDateCreated = true
				)
			),

		Compile / resourceGenerators += GenerateSchemaDefinitions (
			"src/main/raml/database.raml"
			).taskValue,

		Compile / sourceGenerators += runScraml,
		IntegrationTest / parallelExecution := false,
		docker / imageNames := Seq (
			ImageName(s"${organization.value}/${name.value}:latest")
			),

		docker / dockerfile := {
			val appDir = stage.value
			val installDir = "/app"
			val script = executableScriptName.value

			new Dockerfile {
				/// OpenJDK 19 is the base image.
				from ("eclipse-temurin:19-jre-focal")

				/// Allow traffic to the app and administrative ports.
				expose (6890)

				copy (appDir, installDir, chown = "daemon:daemon")

				/// Set the entrypoint to be invoking java with the app.
				entryPoint (
					s"$installDir/bin/$script",
					"--",
					"--docker",
					"--json"
					)
				}
			}
		)
	.dependsOn (
		chassis % "compile->compile;test->test;it->test"
		)
	.enablePlugins (sbtdocker.DockerPlugin, JavaAppPackaging)


//////////////////////////////
/// End-to-End Testing
//////////////////////////////

val gatling = (project in file ("gatling"))
	.settings (
		name := "gatling-tests",
		publish / skip := true,
		libraryDependencies ++=
			Seq (
				Enumeratum,
				Scalacheck,
				ScalacheckShapeless,
				Cats,
				Chimney,
				CirceCore,
				CirceGeneric,
				CirceParser,
				GatlingCore,
				JacksonDatabind,
				Logback,
				LogbackJackson,
				LogbackJson,
				MonocleCore,
				Shapeless
				) ++
			Seq (
				GatlingCharts,
				Scalatest,
				ScalatestScalacheck
				).map (_ % Test) ++
			Seq (
				GatlingCharts
				).map (_ % "it"),

		defaultTypes := scraml.DefaultTypes (
			array = "scala.collection.immutable.Vector",
			dateTime = "java.time.OffsetDateTime",
			float = "Double",
			double = "scala.math.BigDecimal",
			number = "scala.math.BigDecimal",
			time = "java.time.OffsetTime"
			),

		librarySupport := Set (
			scraml.libs.CatsEqSupport,
			scraml.libs.CatsShowSupport,
			scraml.libs.CirceJsonSupport (
				formats = Map (
					"offsetDateTime" -> "io.circe.Decoder.decodeOffsetDateTime",
					"offsetTime" -> "io.circe.Decoder.decodeOffsetTime"
					)
				),

			scraml.libs.MonocleOpticsSupport
			),

		ramlDefinitions := Map (
			"inventory.raml" -> "inventory",
			"company.raml" -> "company",
			"purchase-order.raml" -> "purchaseOrder",
			"storage-facility.raml" -> "storageFacility"
			).map {
				case (ramlFile, packageName) =>

				scraml.ModelDefinition (
					raml = file (s"api/src/main/raml/$ramlFile"),
					basePackage = s"com.github.osxhacker.$systemName.api.$packageName",
					defaultPackageAnnotation = Some("APITypes"),
					fieldMatchPolicy = Some (scraml.FieldMatchPolicy.Exact ()),
					generateDateCreated = true
					)
				}
				.toSeq,

		Compile / sourceGenerators += runScraml,
		IntegrationTest / parallelExecution := false
		)
	.enablePlugins (GatlingPlugin)


def commonServiceSettings (testScopes : String = "test") = Seq (
	libraryDependencies ++=
		Cats ::
		CatsEffect ::
		Chimney ::
		ChimneyCats ::
		CirceCore ::
		CirceGeneric ::
		CirceParser ::
		CirceRefined ::
		Decline ::
		DeclineEffect ::
		DeclineRefined ::
		DiffxCore ::
		DiffxCats ::
		DiffxRefined ::
		Enumeratum ::
		EnumeratumCats ::
		Fs2Core ::
		Fs2Io ::
		Fs2Kafka ::
		Http4sBlaze ::
		JacksonDatabind ::
		Janino ::
		KamonBundle ::
		KamonCatsIo ::
		KamonPrometheus ::
		KamonSystemMetrics ::
		Log4Cats ::
		Logback ::
		LogbackJackson ::
		LogbackJson ::
		LogstashLogbackEncoder ::
		MonocleCore ::
		MonocleGeneric ::
		MonocleMacro ::
		Mouse ::
		Pureconfig ::
		PureconfigCats ::
		Refined ::
		RefinedCats ::
		RefinedPureconfig ::
		RefinedShapeless ::
		Shapeless ::
		TapirCore ::
		TapirHttp4s ::
		TapirJsonCirce ::
		Nil ::: List (
			CatsEffectTesting,
			KamonTestkit,
			Log4CatsNoop,
			RefinedScalacheck,
			Scalacheck,
			ScalacheckShapeless,
			Scalatest,
			SttpClientCirce,
			TapirClient,
			TapirStubServer
			).map (_ % testScopes)
	)


def generateCamelMainProperties (versions : Seq[(String, String)]) = Def.task {
	val log = streams.value.log
	val header = "# Library versions\n"
	val template = ((Compile / resourceDirectory).value / "camel" / "camel-main.properties")
		.get ()
		.headOption
		.map (IO.read (_))
		.getOrElse (header)

	val properties = (Compile / resourceManaged).value / "application.properties"

	log.info (s"generating Camel Main properties file: $properties")

	val contents = versions.foldLeft (new StringBuilder (template)) {
		case (accum, (name, version)) =>
			accum.append ("demo.versions.")
				.append (name)
				.append (" = ")
				.append (version)
				.append ('\n')
		}

	IO.write (properties, contents.toString ())
	Seq (properties)
}

