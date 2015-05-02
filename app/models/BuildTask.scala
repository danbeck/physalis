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

  def startBuilding(): Unit = {
    logger.info(s"Building ${projectName} (gitURL was: ${project.gitUrl}) for platform $platform")

    if (platform == "android") {
      val empty: Array[String] = new Array[String](0)
      logger.info("exec docker/cordova")
      val projectDir = s"${System.getProperty("user.dir")}/$projectPath/GreenMahjong/";
      //            Runtime.getRuntime.exec(s"docker run danielbeck/cordova-android build $platform", empty, new File(projectPath))
      //      val addPlatformCordova = new ProcessBuilder("/usr/bin/docker", "run", "--rm", "danielbeck/cordova-android", "platform", "add", platform)
      //        .directory(new File(projectDir)).start
      //      logProcess(addPlatformCordova)

      logger.info(s"CWD: $projectDir")
      val addcordova = new ProcessBuilder("/usr/bin/docker",
        "run", "--rm", "-v", s"${projectDir}:/data",
        "danielbeck/cordova-android", "platform", "add", platform).start
      logProcess(addcordova)
      val cordova = new ProcessBuilder("/usr/bin/docker",
        "run", "--rm", "-v", s"${projectDir}:/data",
        "danielbeck/cordova-android", "build", platform).start
      logProcess(cordova)
      //      val pwd = new ProcessBuilder("pwd").directory(new File(projectDir)).start
      //      logProcess(pwd)

      //      val command = s"docker run danielbeck/cordova-android ${projectPath} build ${platform}"
      logger.info("done")
    }
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