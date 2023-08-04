package com.github.osxhacker.demo.gatling


/**
 * The '''FeatureSimulationSpec''' type defines the unit-tests which certify
 * [[com.github.osxhacker.demo.gatling.FeatureSimulation]] for fitness of
 * purpose and serves as an exemplar of its use.
 */
final class FeatureSimulationSpec ()
	extends FeatureSimulation ("http://computer-database.gatling.io")
		with CirceAware[SessionKey]
{
	/// Instance Properties
	val loadHomePage = scenario ("Gatling Test Site").exec (
		http ("Get Home Page").get ("/")
			.check (isSuccess)
		)


	evaluate (protocols.html) {
		loadHomePage ::
		Nil
		}
}

