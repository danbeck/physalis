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

  private val projectPath = s"${System.getProperty("user.dir")}/${user.id}/${project.id}/$platform"
  private val logFile = s"$projectPath/log-physalis.txt"
  private val workspace = s"$projectPath/workspace/"
  private val PROMPT = "\nphysalis:~/$"
  private val logger: Logger = Logger(this.getClass)

  private val projectName = project.gitUrl match {
    case BuildTask.validGitUrlRegex(projectName) => projectName
  }

  def save(): BuildTask = {
    logger.info(s"Save BuildTask ${this.id}:${this.state}")
    Repository.saveBuildTask(this)
    logger.info(s"Saved BuildTask  ${this.id}:${this.state}")
    this
  }

  private def updateState(state: String) = this.copy(state = state, updated = Instant.now)

  def execute(): BuildTask = {
    logger.info("Processing buildtask: " + id)
    val buildTaskInprogress = this.inProgress().save()
    buildTaskInprogress.gitClone()
    buildTaskInprogress.build().save()
  }
  def inProgress() = this.updateState("IN_PROGRESS")

  def done(s3Url: String, logS3Url: String) = {
    deleteProjectDir()
    this.updateState("DONE").copy(s3Url = Some(s3Url), logS3Url = Some(logS3Url))
  }

  def error(logS3Url: String): BuildTask = {
    deleteProjectDir()
    this.updateState("ERROR").copy(logS3Url = Some(logS3Url))
  }

  def gitClone(): BuildTask = {
    deleteProjectDir()
    execute("git", "clone", project.gitUrl, "--depth=1", workspace)
    this
  }

  private def deleteProjectDir() = {
    val command = s"rm -rf ${projectPath}"
    logger.info(s">> ${command}")
    command.!
  }

  private def cordovaDir(): Option[File] = {
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles
      these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    if (!new File(workspace).exists)
      None
    else {
      logger.info(s"checkoutDir was: $projectPath")

      val allFiles = recursiveListFiles(new File(workspace))

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
    buildAndUploadToS3() match {
      case Left((error, log)) => {
        logger.error(error)
        this.error(log).save()
      }
      case Right((logFileUrl, s3Url)) => this.done(s3Url = s3Url, logS3Url = logFileUrl)
    }
  }
  private def buildAndUploadToS3(): Either[(String, String), (String, String)] = {
    logger.info(s"Building ${projectName} (gitURL was: ${project.gitUrl}) for platform $platform")

    if (platform == "android" || platform == "ubuntu" || platform == "firefoxos") {
      logger.info(s"Project dir: $cordovaDir")
      cordovaDir match {
        case Some(dir) => buildAndUploadToS3(dir.getAbsolutePath)
        case _ =>
          writeToLogfile("\nPhysalis Error: Couldn't found a Apache Cordova project!" +
            " Make sure that a \"config.xml\" file and a \"www\" directory exist.\n")
          val urlString = S3BucketService.putLog(task = this, file = new File(logFile)).toString
          Left("Couldn't found a Apache Cordova project!", urlString)
      }
    } else Left(s"$platform is not defined", "")
  }

  def buildAndUploadToS3(dir: String): Either[(String, String), (String, String)] = {
    logger.info(s"Building. CWD set to $dir")
    addCordovaPlatform(dir)
    buildWithCordova(dir)
    getBuildArtifact(dir) match {
      case Left((error, file)) =>
        logger.info("Found an error. uploading log to S3")
        val urlString = S3BucketService.putLog(task = this, file = file).toString
        logger.info(s"S3 URL $urlString")
        Left(error, urlString)
      case Right((log, artifact)) =>
        logger.info("Build terminated successully. Uploading log to S3")
        val logFileUrl = S3BucketService.putLog(task = this, file = log).toString
        logger.info(s"S3 URL $logFileUrl")
        logger.info("Uploading build artifact to S3")
        val artifactUrl = S3BucketService.putArtifact(task = this, file = artifact).toString
        logger.info(s"S3 URL $artifactUrl")
        Right(logFileUrl, artifactUrl)
    }
  }

  private def addCordovaPlatform(dir: String) = {
    logger.info("addCordovaPlatform")
    platform match {
      case "ubuntu" =>
        executeInDir(dir, "cordova", "platform", "add", "ubuntu")
      case _ =>
        execute("/usr/bin/docker", "run", "--rm", "-v", s"${dir}:/data",
          "danielbeck/cordova-android:5.0.0", "platform", "add", platform)
    }
  }

  private def buildWithCordova(dir: String) = {
    logger.info("buildWithCordova")
    if (platform == "android")
      execute("/usr/bin/docker", "run", "--rm", "-v", s"${dir}:/data",
        "danielbeck/cordova-android:5.0.0", "build", platform)

    if (platform == "ubuntu")
      executeInDir(dir, "cordova", "build", platform, "--device")

    if (platform == "firefoxos")
      execute("/usr/bin/docker", "run", "--rm", "--privileged", "-v", s"${dir}:/data",
        "danielbeck/cordova-firefoxos", "build", platform)

  }

  private def execute(command: String*): java.lang.Process = {
    writeToLogfile(s"""$PROMPT ${command.mkString(" ")}""" + "\n")
    val process = new ProcessBuilder(command: _*).start
    logProcess(process)
    process
  }

  private def executeInDir(dir: String, command: String*): java.lang.Process = {
    writeToLogfile(s"""$PROMPT ${command.mkString(" ")}""" + "\n")
    val process = new ProcessBuilder(command: _*).directory(new File(dir)).start
    logProcess(process)
    process
  }

  private def getBuildArtifact(dir: String): Either[(String, File), (File, File)] = {
    platform match {
      case "android"   => getAndroidBuildArtifact(dir)
      case "ubuntu"    => getUbuntuBuildArtifact(dir)
      case "firefoxos" => getFirefoxosBuildArtifact(dir)
      case _ =>
        Left("platform not defined", new File(logFile))
    }
  }

  private def getAndroidBuildArtifact(dir: String): Either[(String, File), (File, File)] = {
    val path = s"$dir/platforms/$platform/build/outputs/apk/android-debug.apk"
    buildArtifact(Some(path))
  }

  private def getUbuntuBuildArtifact(dir: String) = {
    val outputDir = s"$dir/platforms/ubuntu/ubuntu-sdk-14.10/armhf/prefix/"
    new File(outputDir) match {
      case file if file.exists() =>
        val outputFile = file.listFiles().map(_.getAbsolutePath).filter { _.endsWith(".click") }.headOption
        buildArtifact(outputFile)
      case _ => buildArtifact(None)
    }
  }

  private def getFirefoxosBuildArtifact(dir: String) = {
    val path = s"$dir/platforms/firefoxos/build/package.zip"
    buildArtifact(Some(path))
  }

  private def buildArtifact(pathToArtifact: Option[String]): Either[(String, File), (File, File)] = {
    logger.info(s"search $platform build artifact $pathToArtifact")
    //    val outputFile = new File(s"$dir/platforms/$platform/ant-build/MainActivity-debug.apk")
    val log = new File(logFile)
    pathToArtifact match {
      case None => Left(s"Building failed - the build artifact was not found!", log)

      case Some(file) if (new File(file) exists) =>
        logger.info(s"file $pathToArtifact exists")
        Right(log, new File(file))

      case Some(file) => Left(s"Building failed - the build artifact at location $pathToArtifact was not found!", log)

    }
  }

  def logProcess(p: java.lang.Process) = {
    logStream(p.getErrorStream)
    logStream(p.getInputStream)
  }

  def logStream(stream: InputStream) = {
    val br = new BufferedReader(new InputStreamReader(stream))
    val str: String = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
    writeToLogfile(str)
  }

  def writeToLogfile(string: String) = {
    try {
      logger.info(string)
      if (!new File(logFile).exists) {
        logger.info("creating file " + logFile)
        new File(logFile).getParentFile.mkdirs()
        new File(logFile).createNewFile()
      }

      Files.write(Paths.get(logFile), string.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    } catch {
      case e: Exception => logger.error("There was an exception", e)
    }
  }
}

object BuildTask {
  def findNew(platforms: Seq[String]) = Repository.findNewBuildTasks(platforms)
  val validGitUrlRegex = """https?://.*/(.*)\.git""".r
  val validGitUrlPattern = validGitUrlRegex.pattern

}