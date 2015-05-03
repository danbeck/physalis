package service
import awscala._, s3._
import java.net.URL

object S3BucketService {
  implicit val s3 = S3()
  val buckets: Seq[Bucket] = s3.buckets
  val bucket: Bucket = s3.bucket("io.physalis").get

  def putFile(userId: String, projectId: String, version: String, file: File): Unit = {
    bucket.delete(s"$userId/$projectId/$version")
    val buck = bucket.put(s"$userId/$projectId/$version", file)
  }

  def getBucketURL(userId: String, projectId: String, version: String): URL = {
    val s3obj: Option[S3Object] = bucket.getObject(s"$userId/$projectId/$version")
    s3obj.get.generatePresignedUrl(new DateTime().plusHours(1))
  }
}