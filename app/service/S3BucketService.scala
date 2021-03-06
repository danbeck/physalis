package service
import awscala._, s3._
import java.net.URL
import models.BuildTask
import play.api.Logger

object S3BucketService {
  implicit val s3 = S3()

  private val isProd = play.api.Play.isProd(play.api.Play.current)
  private val BUCKET_NAME = if (isProd) "physalis" else "physalisdev"
  private val CMS_BUCKET_NAME = "physalis-cms" 
  private val logger: Logger = Logger(this.getClass)
  val buckets: Seq[Bucket] = s3.buckets
  val bucket: Bucket = s3.bucket(BUCKET_NAME).get
  val cmsbucket: Bucket = s3.bucket(CMS_BUCKET_NAME).get
  
  def artifactKey(task: BuildTask, version: String, file: File) = {
    task.platform match {
      case "android"   => s"${task.project.userId}/${task.project.id}/$version/${task.project.name}.apk"
      case "ubuntu"    => s"${task.project.userId}/${task.project.id}/$version/${file.getName}"
      case "firefoxos" => s"${task.project.userId}/${task.project.id}/$version/${file.getName}"
    }
  }

  def logKey(task: BuildTask, version: String) = {
    s"${task.project.userId}/${task.project.id}/$version/${task.project.name}/${task.platform}/log-physalis.txt"
  }

  def putArtifact(task: BuildTask, version: String = "latest", file: File): URL = {
    val k = artifactKey(task, version, file)
    logger.info(s"Uploading build to S3: $k")
    put(k, file)
  }

  def putLog(task: BuildTask, version: String = "latest", file: File): URL = {
    val k = logKey(task, version)
    logger.info(s"Uploading log file to S3: $k")
    put(k, file)
  }

  private def put(key: String, file: File): URL = {
    bucket.delete(key)
    bucket.put(key, file)
    bucket.getObject(key).get.generatePresignedUrl(new DateTime().plusWeeks(1))
  }

  def getLogURL(task: BuildTask, version: String = "latest"): URL = {
    val k = logKey(task, version)
    get(k)
  }

  private def get(key: String): URL = {
    val s3obj: Option[S3Object] = bucket.getObject(key)
    s3obj.get.generatePresignedUrl(new DateTime().plusWeeks(1))
  }

  def getCMS(key: String): Option[URL] = {
    val s3obj: Option[S3Object] = cmsbucket.getObject(key)
    s3obj.map(_.publicUrl)
  }
}