package controllers

import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService
import play.api.mvc.Action
import service.S3BucketService
import play.twirl.api.Html
import java.net.URL
import play.api.Logger

class AmazonS3StaticController(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  private val log: Logger = Logger(this.getClass)

  def html(file: String) = UserAwareAction { implicit request =>
    val urlOption: Option[URL] = S3BucketService.getCMS(s"cms/$file/index.html")
    log.info(s"retrieving $urlOption")
    urlOption match {
      case Some(url) =>
        val string = scala.io.Source.fromURL(url).mkString
        Ok(views.html.training.training(request.user)(Html(string)))
      case None =>
        Ok(views.html.training.training(request.user)(Html("Could'nt found the file: " + file)))
    }
  }
}