package com.github.osxhacker.demo

import scala.collection.JavaConverters._

import io.vrap.rmf.raml.model._
import io.vrap.rmf.raml.model.modules.Api
import io.vrap.rmf.raml.model.types._
import org.eclipse.emf.common.util.URI
import sbt._
import sbt.Keys._
import sbt.internal.util.ManagedLogger


/**
 * The '''GenerateSchemaDefinitions''' type provides the ability to scan all
 * [[https://github.com/raml-org/raml-spec/blob/master/versions/raml-10/raml-10.md/ RAML]]
 * resources for the `(schema)` custom annotation and then, for all having it,
 * generate SQL resource files accordingly.
 *
 * The algorithm creates a dynamic [[sbt.Task]] which:
 *
 *   - Loads the '''main''' `.raml` definition from the [[sbt.Project]].
 *
 *   - Filters each RAML type to retain only those which have a `(schema)`
 *     element.
 *
 *   - Extracts the ''String'' content of `(schema)`.
 *
 *   - Creates a `.sql` file in the target resources with the `displayName` and
 *     `.sql` suffix.
 */
object GenerateSchemaDefinitions
{
	def apply (relativeMain : String) : Def.Initialize[Task[Seq[File]]] =
		Def.task {
			val log = streams.value.log
			val main = baseDirectory.value / relativeMain
			val target = (Compile / resourceManaged).value
			val api = loadRamlModel (main).fold (e => throw e, identity)

			log.info (s"generating schema definitions for: $main")

			findTypesWithSchema (api, log)
				.map (extractNameAndSchema)
				.map (generateSqlResource (target, log).tupled)
			}


	private def extractNameAndSchema (objectType : ObjectType)
		: (String, String) =
	{
		val typeName = objectType.getName
		val schema = Option (objectType.getAnnotation ("schema"))
			.map (_.getValue.getValue.toString)

		typeName -> schema.getOrElse (throw new RuntimeException ("missing schema"))
	}


	private def findTypesWithSchema (
		api : RamlModelResult[Api],
		log : ManagedLogger
		)
		: Seq[ObjectType] =
		api.getRootObject
			.getTypes
			.asScala
			.collect {
				case ot : ObjectType if ot.getAnnotation ("schema") ne null =>
					log.info (s"found schema in: ${ot.getName}")
					ot
			}


	private def generateSqlResource (targetDir : File, log : ManagedLogger)
		: (String, String) => File =
		(name, sql) => {
			val destination = targetDir / s"$name.sql"

			log.info (s"generating schema: $destination")
			IO.write (destination, sql)
			destination
			}


	private def loadRamlModel (main : File)
		: Either[RuntimeException, RamlModelResult[Api]] =
	{
		val result = new RamlModelBuilder ().buildApi (
			URI.createFileURI (main.getPath)
			)

		val validations = result.getValidationResults
			.asScala
			.toList

		Either.cond (
			validations.isEmpty,
			result,
			new RuntimeException (
				validations.map (_.getMessage)
					.mkString ("\n")
				)
			)
	}
}
