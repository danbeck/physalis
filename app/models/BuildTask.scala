package models

import play.api.Logger
import service.simpledb.Repository
import scala.sys.process._
import java.util.UUID
import java.lang.ProcessBuilder
import java.io.File
import java.lang.ProcessBuilder.Redirect
import java.io.InputStreamReader
import java.io.BufferedReader

/**
 * Created by daniel on 23.04.15.
 */
case class BuildTask(
  id: String = UUID.randomUUID().toString(),
  project: Project,
  user: User,
  state: String = "NEW",
  s3Url: Option[String] = None,
  platform: String) {

  private val projectPath = s"${user.id}/${project.id}/$platform"
  private val logger: Logger = Logger(this.getClass)

  private val projectName = project.gitUrl match {
    case BuildTask.validGitUrlRegex(projectName) => projectName
  }

  def save(): BuildTask = {
    logger.info(s"Save BuildTask ${this.id}:${this.state}")
    Repository.saveBuildTask(this)
  }

  def inProgress(): BuildTask = this.copy(state = "IN_PROGRESS")

  def done(s3Url: Option[String] = None): BuildTask = this.copy(state = "DONE", s3Url = s3Url)

  def error(): BuildTask = this.copy(state = "ERROR")

  def gitClone() = {
    val command = s"rm -rf ${projectPath}"
    Logger.info(s">> ${command}")
    command.!
    val cloneCommand = s"git clone ${project.gitUrl} --depth=1 ${projectPath}"
    Logger.info(s">> ${cloneCommand}")
    cloneCommand.!
  }

  private def cordovaDir(): Option[File] = {
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    val checkoutDir = new File(s"${System.getProperty("user.dir")}/$projectPath/")
    recursiveListFiles(checkoutDir)
      .filter(_.isDirectory())
      .filter(_.getName == "www")
      .filter(_.listFiles.exists { file => file.getName == "config.xml" })
      .sortWith(_.getName.count(_ == '/') < _.getName.count(_ == '/'))
      .map(_.getParentFile)
      .headOption
  }
  def startBuilding(): Unit = {
    logger.info(s"Building ${projectName} (gitURL was: ${project.gitUrl}) for platform $platform")

    if (platform == "android") {
      cordovaDir match {
        case Some(dir) => build(dir.getAbsolutePath)
        case _         => logger.error("Could not found a cordova project !")
      }
      logger.info("done")
    }
  }

  def build(dir: String) = {
    logger.info(s"Building dir $dir")
    val addcordova = new ProcessBuilder("/usr/bin/docker",
      "run", "--rm", "-v", s"${dir}:/data",
      "danielbeck/cordova-android", "platform", "add", platform).start
    logProcess(addcordova)
    val cordova = new ProcessBuilder("/usr/bin/docker",
      "run", "--rm", "-v", s"${dir}:/data",
      "danielbeck/cordova-android", "build", platform).start
    logProcess(cordova)
  }

  def logProcess(p: java.lang.Process) = {
    val br = new BufferedReader(new InputStreamReader(p.getErrorStream()))
    val str = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
    logger.info("EE: " + str) //      var line: String = null
    val brS = new BufferedReader(new InputStreamReader(p.getInputStream))
    val strS = Stream.continually(brS.readLine()).takeWhile(_ != null).mkString("\n")
    logger.info("II: " + strS) //      var line: String = null

  }

}

object BuildTask {
  def findNew(platform: String) = Repository.findNewBuildTasks(platform)
  val validGitUrlRegex = """https?://.*/(.*)\.git""".r
  val validGitUrlPattern = validGitUrlRegex.pattern
  def createNew(project: Project, user: User) = {
    //    BuildTask()
  }

}