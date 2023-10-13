package com.github.osxhacker.demo.storageFacility

import org.apache.camel.main.Main


/**
 * The '''Server''' `object` defines the entry-point for the storage facility
 * related frontend microsite.
 */
object Server
{
	/// Instance Properties
	private val main = new Main (getClass)
	private val resources =
		"utilities/*.xml" ::
		"templates/*.xml" ::
		"rest/*.xml" ::
		Nil


	def main (args : Array[String]) : Unit =
	{
		main.configure ()
			.withRoutesIncludePattern (
				resources.map ("camel/" + _)
					.mkString (",")
				)

		main.run (args)
	}
}

