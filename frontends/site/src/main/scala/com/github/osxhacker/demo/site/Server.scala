package com.github.osxhacker.demo.site

import org.apache.camel.main.Main


/**
 * The '''Server''' `object` defines the entry-point for the frontend home, or
 * root, microsite.
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

