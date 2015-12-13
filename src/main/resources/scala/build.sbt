name := "ggp-base-scala"

version := "0.1"



scalaVersion := "2.11.7"



libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"


libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.12.5" % "test"

externalDependencyClasspath in Compile += baseDirectory.value / "../../../../build/classes/main"
