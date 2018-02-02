import sbt._

trait CommonBuild extends Build {
  
  lazy val scalaTestDependency = "org.scalatest" %% "scalatest" % "2.2.4"

}
