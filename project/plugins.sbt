logLevel := Level.Warn

// code coverage plugins
resolvers += Classpaths.sbtPluginReleases
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.3")
