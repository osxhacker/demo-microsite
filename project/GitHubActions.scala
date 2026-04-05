package com.github.osxhacker.demo

import sbt._
import sbt.Keys._
import sbtghactions._
import sbtghactions.GenerativeKeys._


/**
 * The '''ConfigureGitHubActions''' `object` is defined as an [[sbt.AutoPlugin]]
 * to ensure [[sbtghactions.GenerativePlugin]] is available and initialized
 * before augmenting its settings.  This is particularly relevant for having to
 * add permissions to the "publish" [[sbtghactions.WorkflowJob]].
 */
object ConfigureGitHubActions
	extends AutoPlugin
{
	/// Instance Properties
	override lazy val requires = GenerativePlugin
	override lazy val trigger = allRequirements

	/// see: https://github.com/sbt/sbt-github-actions
	override lazy val buildSettings : Seq[Setting[_]] =
		Seq (
			ThisBuild / githubWorkflowBuild :=
				compileThenTest ::
				Nil,

			ThisBuild / githubWorkflowBuildPreamble :=
				detectVersionDuplicates ::
				Nil,

			ThisBuild / githubWorkflowEnv ++= additionalEnv,

			/// sbt-github-actions defaults to using JDK 8 for testing and
			/// publishing.
			ThisBuild / githubWorkflowJavaVersions := JavaSpec.temurin ("25") ::
				Nil,

			ThisBuild / githubWorkflowGeneratedCI := addPermissions ().value,

			ThisBuild / githubWorkflowPublishTargetBranches :=
				RefPredicate.Equals (Ref.Branch ("main")) ::
				RefPredicate.Equals (Ref.Branch ("master")) ::
				Nil,

			ThisBuild / githubWorkflowPublishPreamble :=
				detectSnapshotVersions ::
				Nil,
			)


	private lazy val additionalEnv = Map (
		"JAVA_OPTS" -> javaOpts.mkString (" ")
		)

	private lazy val buildPermissions = Permissions.Specify (
		values = Map (
			PermissionScope.Contents -> PermissionValue.Read,
			PermissionScope.Packages -> PermissionValue.Read
			)
		)

	private lazy val compileThenTest = WorkflowStep.Sbt (
		name = Some ("Build project"),
		commands =
			"compile" ::
			"test" ::
			Nil
		)

	private lazy val detectSnapshotVersions = WorkflowStep.Run (
		name = Some ("Reject attempts to publish snapshots"),
		commands =
			"""test
			|"`grep -i '^\s*thisbuild\s*/\s*version\s*:=.*snapshot' build.sbt`"
			|= "" || {
			|"""
			.stripMargin
			.replaceAll ("\n", " ")
			.trim ::
			"  echo 'Publishing snapshots is not supported' > /dev/stderr" ::
			"  exit 1" ::
			"  }" ::
			Nil
		)

	private lazy val detectVersionDuplicates = WorkflowStep.Run (
		name = Some ("Reject attempts to build duplicate versions"),
		commands =
			"/bin/sh -e scripts/detect-version-duplicates" ::
			Nil
		)

	private lazy val javaOpts =
		"-Xms2048M" ::
		"-Xmx4096M" ::
		"-Xss2M" ::
		"-XX:ReservedCodeCacheSize=256m" ::
		"-XX:+UseG1GC" ::
		"-server" ::
		"-Dfile.encoding=UTF-8" ::
		Nil

	private lazy val publishPermissions = Permissions.Specify (
		values = Map (
			PermissionScope.Contents -> PermissionValue.Read,
			PermissionScope.Packages -> PermissionValue.Write
			)
		)


	private def addPermissions () = Def.setting {
		githubWorkflowGeneratedCI.value.map {
			case publish : WorkflowJob if publish.id == "publish" =>
				publish.copy (permissions = Some (publishPermissions))

			case build : WorkflowJob if build.id == "build" =>
				build.copy (permissions = Some (buildPermissions))

			case unchanged =>
				unchanged
			}
		}
}

