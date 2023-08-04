package com.github.osxhacker.demo.gatling.service.company.task

import io.gatling.core.config.GatlingConfiguration

import com.github.osxhacker.demo.gatling.Task
import com.github.osxhacker.demo.gatling.service.company.{
	CompanySessionKeys,
	ResourceOptics
	}


/**
 * The '''CompanyTask''' type defines the common ancestor to __all__
 * [[com.github.osxhacker.demo.api.company]]-based
 * [[https://gatling.io/docs/gatling/tutorials/quickstart/ Gatling]]
 * [[com.github.osxhacker.demo.gatling.Task]]s.  Note that the `configuration`
 * property __must__ be available for use by [[io.gatling.core.CoreDsl]].
 */
abstract class CompanyTask ()
	(implicit override val configuration : GatlingConfiguration)
	extends Task[CompanySessionKeys]
		with ResourceOptics
