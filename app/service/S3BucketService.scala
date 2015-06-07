package service
import awscala._, s3._
import java.net.URL
import models.BuildTask
import play.api.Logger

object S3BucketService {
  implicit val s3 = S3()
  private val BUCKET_NAME = "io.physalis"
  val buckets: Seq[Bucket] = s3.buckets
  val bucket: Bucket = s3.bucket(BUCKET_NAME).get
  val logger: Logger = Logger(this.getClass)

  def artifactKey(task: BuildTask, version: String) = {
    val fileEnding = task.platform match {
      case "android" => "apk"
      case "ubuntu"  => "click"
    }
    s"${task.project.userId}/${task.project.id}/$version/${task.project.name}.$fileEnding"
  }

  def logKey(task: BuildTask, version: String) = {
    val fileEnding = task.platform match {
      case "android" => "apk"
      case "ubuntu"  => "click"
    }
    s"${task.project.userId}/${task.project.id}/$version/${task.project.name}/${task.platform}/log.txt"
  }

  def putArtifact(task: BuildTask, version: String = "latest", file: File): URL = {
    val k = artifactKey(task, version)
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

  def getArtifactURL(task: BuildTask, version: String = "latest"): URL = {
    val k = artifactKey(task, version)
    get(k)
  }

  def getLogURL(task: BuildTask, version: String = "latest"): URL = {
    val k = logKey(task, version)
    get(k)
  }

  private def get(key: String): URL = {
    val s3obj: Option[S3Object] = bucket.getObject(key)
    s3obj.get.generatePresignedUrl(new DateTime().plusWeeks(1))
  }
}