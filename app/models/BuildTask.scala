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
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import java.nio.charset.StandardCharsets
/**
 * Created by daniel on 23.04.15.
 */
case class BuildTask(
    id: String = UUID.randomUUID().toString(),
    project: Project,
    user: User,
    state: String = "NEW",
    s3Url: Option[String] = None,
    logS3Url: Option[String] = None,
    platform: String,
    created: Instant = Instant.now,
    updated: Instant = Instant.now) {

  private val projectPathRelative = s"${user.id}/${project.id}/$platform"
  private val projectPath = new File(s"${System.getProperty("user.dir")}/$projectPathRelative/")
  private val logFile = s"$projectPath/log.txt"

  private val logger: Logger = Logger(this.getClass)

  private val projectName = project.gitUrl match {
    case BuildTask.validGitUrlRegex(projectName) => projectName
  }

  def save(): BuildTask = {
    logger.info(s"Save BuildTask ${this.id}:${this.state}")
    Repository.saveBuildTask(this)
  }

  private def updateState(state: String) = this.copy(state = state, updated = Instant.now)

  def execute(): BuildTask = {
    logger.info("Processing buildtask: " + id)
    val buildTaskInprogress = this.inProgress().save()
    buildTaskInprogress.gitClone()
    buildTaskInprogress.build().save()
  }
  def inProgress() = this.updateState("IN_PROGRESS")

  def done(s3Url: String, logS3Url: String) = this.updateState("DONE").copy(s3Url = Some(s3Url), logS3Url = Some(logS3Url))

  def error(logS3Url: String): BuildTask = this.updateState("ERROR").copy(logS3Url = Some(logS3Url))

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

    if (!projectPath.exists)
      None
    else {
      logger.info(s"checkoutDir was: $projectPath")

      val allFiles = recursiveListFiles(projectPath)

      allFiles
        .filter(d => d.isDirectory() && d.getName == "www")
        .filter(_.listFiles.exists { _.getName == "config.xml" })
        .union(allFiles
          .filter(f => !f.isDirectory() && f.getName == "config.xml")
          .filter(_.getParentFile.listFiles.exists { d => d.getName == "www" && d.isDirectory }))
        .sortWith(_.getName.count(_ == '/') < _.getName.count(_ == '/'))
        .map(_.getParentFile)
        .headOption
    }
  }

  def build(): BuildTask = {
    buildAndUploadToS3 match {
      case Left((error, log)) => {
        logger.error(error)
        this.error(log)
      }
      case Right((log, s3Url)) => this.done(s3Url = s3Url, logS3Url = log)
    }
  }
  private def buildAndUploadToS3(): Either[(String, String), (String, String)] = {
    logger.info(s"Building ${projectName} (gitURL was: ${project.gitUrl}) for platform $platform")

    if (platform == "android" || platform == "ubuntu") {
      cordovaDir match {
        case Some(dir) => buildAndUploadToS3(dir.getAbsolutePath)
        case _         => Left("Couldn't found a Apache Cordova project!", null)
      }
    } else Left(s"$platform ist not defined", "")
  }

  def buildAndUploadToS3(dir: String): Either[(String, String), (String, String)] = {
    logger.info(s"Building. CWD set to $dir")
    addCordovaPlatform(dir)
    buildWithCordova(dir)
    getBuildArtifact(dir) match {
      case Left((error, file)) =>
        logger.info("Uploading log to S3")
        val urlString = S3BucketService.putLog(task = this, file = file).toString
        logger.info(s"S3 URL $urlString")
        Left(error, urlString)
      case Right((log, artifact)) =>
        logger.info("Uploading log to S3")
        val logString = S3BucketService.putLog(task = this, file = log).toString
        logger.info(s"S3 URL $logString")
        logger.info("Uploading file to S3")
        val urlString = S3BucketService.putArtifact(task = this, file = artifact).toString
        logger.info(s"S3 URL $urlString")
        Right(logString, urlString)
    }
  }

  private def addCordovaPlatform(dir: String) = {
    logger.info("addCordovaPlatform")
    execute("/usr/bin/docker", "run", "--rm", "-v", s"${dir}:/data",
      "danielbeck/cordova-android:5.0.0", "platform", "add", platform)
  }

  private def buildWithCordova(dir: String) = {
    logger.info("buildWithCordova")
    if (platform == "android")
      execute("/usr/bin/docker", "run", "--rm", "-v", s"${dir}:/data",
        "danielbeck/cordova-android:5.0.0", "build", platform)

    if (platform == "ubuntu")
      execute("cordova", "build", platform)

    if (platform == "firefoxos")
      execute("/usr/bin/docker", "run", "--rm", "--privileged", "-v", s"${dir}:/data",
        "danielbeck/cordova-firefoxos", "build", platform)

  }

  private def execute(command: String*): java.lang.Process = {
    val process = new ProcessBuilder(command: _*).start
    logProcess(process)
    process
  }

  private def getBuildArtifact(dir: String): Either[(String, File), (File, File)] = {
    if (platform == "android") {
      getAndroidBuildArtifact(dir)
    } else {
      logger.info("That was not android " + platform)
      Left("platform not defined", new File(logFile))
    }
  }

  private def getAndroidBuildArtifact(dir: String): Either[(String, File), (File, File)] = {
    val path = s"$dir/platforms/$platform/build/outputs/apk/android-debug.apk"
    logger.info(s"search android build artifact $path")
    //    val outputFile = new File(s"$dir/platforms/$platform/ant-build/MainActivity-debug.apk")
    val outputFile = new File(path)
    val log = new File(logFile)
    if (outputFile.exists) {
      logger.info("file exists")
      Right(log, outputFile)
    } else
      Left("Building failed!", log)
  }

  private def getUbuntuBuildArtifact(dir: String) = {
    logger.info(s"search android build artifact $dir/platforms/$platform/ant-build/MainActivity-debug.apk")
    //    val outputDir  = new File(s"$dir/platforms/ubuntu/ubuntu-sdk-14.10/armhf/prefix/")
    //    
    //    if (outputDir.exists)
    //    {
    //      outputDir.listFiles.filter { _.getName.matches(""".*\.click""")}.headOption.fl
    //    }
    //    else 
    val outputFile = new File(s"$dir/platforms/ubuntu/ubuntu-sdk-14.10/armhf/prefix/MainActivity-debug.apk")
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
    val str: String = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
    logger.info(str)
    logger.info("Append logging to file: " + logFile)
    try {
      Files.write(Paths.get(logFile), str.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    } catch {
      case e: Exception => logger.error("There was an exception", e)
    }
    logger.info("Done writing file")
  }
}

object BuildTask {
  def findNew(platforms: Seq[String]) = Repository.findNewBuildTasks(platforms)
  val validGitUrlRegex = """https?://.*/(.*)\.git""".r
  val validGitUrlPattern = validGitUrlRegex.pattern
  //  def createNew(project: Project, user: User) = {
  //    //    BuildTask()
  //  }

}