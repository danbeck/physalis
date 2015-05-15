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
import java.io.InputStream
import service.S3BucketService
import java.net.URL
import java.time.Instant
/**
 * Created by daniel on 23.04.15.
 */
case class BuildTask(
  id: String = UUID.randomUUID().toString(),
  project: Project,
  user: User,
  state: String = "NEW",
  s3Url: Option[String] = None,
  platform: String,
  created: Instant = Instant.now,
  updated: Instant = Instant.now) {

  private val projectPath = s"${user.id}/${project.id}/$platform"
  private val logger: Logger = Logger(this.getClass)

  private val projectName = project.gitUrl match {
    case BuildTask.validGitUrlRegex(projectName) => projectName
  }

  def save(): BuildTask = {
    logger.info(s"Save BuildTask ${this.id}:${this.state}")
    Repository.saveBuildTask(this)
  }

  private def updateState(state: String) = this.copy(state = state, updated = Instant.now)

  def inProgress() = this.updateState("IN_PROGRESS")

  def done(s3Url: String) = this.updateState("DONE").copy(s3Url = Some(s3Url))

  def error(): BuildTask = this.updateState("ERROR")

  def gitClone(): BuildTask = {
    val command = s"rm -rf ${projectPath}"
    Logger.info(s">> ${command}")
    command.!
    val cloneCommand = s"git clone ${project.gitUrl} --depth=1 ${projectPath}"
    Logger.info(s">> ${cloneCommand}")
    cloneCommand.!
    this
  }

  private def cordovaDir(): Option[File] = {
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    val checkoutDir = new File(s"${System.getProperty("user.dir")}/$projectPath/")

    if (!checkoutDir.exists)
      None
    else {
      logger.info(s"checkoutDir was: $checkoutDir")
      recursiveListFiles(checkoutDir)
        .filter(_.isDirectory())
        .filter(_.getName == "www")
        .filter(_.listFiles.exists { _.getName == "config.xml" })
        .sortWith(_.getName.count(_ == '/') < _.getName.count(_ == '/'))
        .map(_.getParentFile)
        .headOption
    }
  }

  def build(): BuildTask = {
    buildAndUploadToS3 match {
      case Left(error) => {
        logger.error(error)
        this.error
      }
      case Right(s3Url) => this.done(s3Url)
    }
  }
  private def buildAndUploadToS3(): Either[String, String] = {
    logger.info(s"Building ${projectName} (gitURL was: ${project.gitUrl}) for platform $platform")

    if (platform == "android" || platform == "ubuntu") {
      cordovaDir match {
        case Some(dir) => buildAndUploadToS3(dir.getAbsolutePath)
        case _         => Left("Couldn't found a Apache Cordova project!")
      }
    } else Left(s"$platform ist not defined")
  }

  def buildAndUploadToS3(dir: String): Either[String, String] = {
    logger.info(s"Building. CWD set to $dir")
    addCordovaPlatform(dir)
    buildWithCordova(dir)
    getBuildArtifact(dir).right.map { file =>
      logger.info("Uploading file to S3")
      val urlString = S3BucketService.putFile(task = this, file = file).toString
      logger.info(s"S3 URL $urlString")
      urlString
    }
  }

  private def addCordovaPlatform(dir: String) = {
    logger.info("addCordovaPlatform")
    execute("/usr/bin/docker", "run", "--rm", "-v", s"${dir}:/data",
      "danielbeck/cordova-android", "platform", "add", platform)
  }

  private def buildWithCordova(dir: String) = {
    logger.info("buildWithCordova")
    if (platform == "android")
      execute("/usr/bin/docker", "run", "--rm", "-v", s"${dir}:/data",
        "danielbeck/cordova-android", "build", platform)

    if (platform == "ubuntu")
      execute("/usr/bin/docker", "run", "--rm", "--privileged", "-v", s"${dir}:/data",
        "danielbeck/cordova-ubuntu", "build", platform)

  }

  private def execute(command: String*): java.lang.Process = {
    val process = new ProcessBuilder(command: _*).start
    logProcess(process)
    process
  }

  private def getBuildArtifact(dir: String): Either[String, File] = {
    if (platform == "android") {
      getAndroidBuildArtifact(dir)
    } else {
      logger.info("That was not android " + platform)
      Left("platform not defined")
    }
  }

  private def getAndroidBuildArtifact(dir: String) = {
    logger.info(s"search android build artifact $dir/platforms/$platform/ant-build/MainActivity-debug.apk")
    val outputFile = new File(s"$dir/platforms/$platform/ant-build/MainActivity-debug.apk")
    if (outputFile.exists) {
      logger.info("file exists")
      Right(outputFile)
    } else
      Left("Building failed!")
  }

  def logProcess(p: java.lang.Process) = {
    logStream(p.getErrorStream)
    logStream(p.getInputStream)
  }

  def logStream(stream: InputStream) = {
    val br = new BufferedReader(new InputStreamReader(stream))
    val str = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
    logger.info(str)
  }
}

object BuildTask {
  def findNew(platform: String) = Repository.findNewBuildTasks(platform)
  val validGitUrlRegex = """https?://.*/(.*)\.git""".r
  val validGitUrlPattern = validGitUrlRegex.pattern
  //  def createNew(project: Project, user: User) = {
  //    //    BuildTask()
  //  }

}