package com.github.osxhacker.demo

import sbt._
import sbt.Keys._
import sbtghactions._
import sbtghactions.GenerativeKeys._


object ConfigureGitHubActions
{
	/// Instance Properties
	private lazy val detectSnapshotVersions = WorkflowStep.Run (
		name = Some ("Reject attempts to publish snapshots"),
		commands =
			"""grep -qi
			|'^\s*thisbuild\s*/\s*version\s*:=.*snapshot'
			|build.sbt
			|"""
			.stripMargin
			.replaceAll ("\n", " ")
			.trim ::
			"" ::
			"test $? -ne 0 || {" ::
			"  echo 'Publishing snapshots is not supported' > /dev/stderr" ::
			"  exit 1" ::
			"  }" ::
			Nil
		)


	/// see: https://github.com/sbt/sbt-github-actions
	def apply () : Seq[Def.Setting[_]] =
		Seq (
			/// sbt-github-actions defaults to using JDK 8 for testing and
			/// publishing.
			ThisBuild / githubWorkflowJavaVersions := JavaSpec.temurin ("25") ::
				Nil,

			ThisBuild / githubWorkflowPublishTargetBranches :=
				RefPredicate.Equals (Ref.Branch ("main")) ::
				RefPredicate.Equals (Ref.Branch ("master")) ::
				Nil,

			ThisBuild / githubWorkflowPublishPreamble :=
				detectSnapshotVersions ::
				Nil
			)
}

