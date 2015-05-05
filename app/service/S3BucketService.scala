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

  def key(task: BuildTask, version: String) =
    s"${task.project.userId}/${task.project.id}/$version/${task.project.name}"

  def putFile(task: BuildTask, version: String = "latest", file: File): URL = {
    val k = key(task, version)
    bucket.delete(k)
    bucket.put(k, file)
    bucket.getObject(k).get.generatePresignedUrl(new DateTime().plusWeeks(1))
  }

  def getBucketURL(task: BuildTask, version: String = "latest"): URL = {
    val k = key(task, version)
    val s3obj: Option[S3Object] = bucket.getObject(k)
    s3obj.get.generatePresignedUrl(new DateTime().plusWeeks(1))
  }
}