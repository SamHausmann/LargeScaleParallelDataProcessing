import sbt._
import Keys._
import Settings._

import java.io.{File, IOException, FileInputStream}
import org.apache.commons.codec.binary.Base64

import scala.util.parsing.json.JSON
import scalaj.http._

import scala.util.{Try, Success, Failure}

case class AssignmentInfo(
  key: String,
  itemId: String,
  premiumItemId: Option[String],
  partId: String,
  styleSheet: Option[File => File]
)

case class MapMapString (map: Map[String, Map[String, String]])
/**
  * Note: keep this class concrete (i.e., do not convert it to abstract class or trait).
  */
class StudentBuildLike protected() extends CommonBuild {

  lazy val root = project.in(file(".")).settings(
    styleCheckSetting,
    libraryDependencies += scalaTestDependency
  ).settings()

  /** *****************************************************************
    * RUNNING WEIGHTED SCALATEST & STYLE CHECKER ON DEVELOPMENT SOURCES
    */

  val styleCheck = TaskKey[Unit]("styleCheck")
  val styleCheckSetting = styleCheck := {
    val (_, sourceFiles) = ((compile in Compile).value, (sources in Compile).value)
    val logger = streams.value.log
    val (feedback, score) = StyleChecker.assess(sourceFiles)
    logger.info(
      s"""|$feedback
          |Style Score: $score out of ${StyleChecker.maxResult}""".stripMargin)
  }

}
