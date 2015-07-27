package controllers

import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService
import play.api.mvc.Action
import service.S3BucketService
import play.twirl.api.Html
import java.net.URL
import play.api.Logger
import play.api.mvc.Result

class AmazonS3StaticController(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  private val log: Logger = Logger(this.getClass)

  def html(filename: String) = UserAwareAction { implicit request =>
    filename match {
      case string if string.endsWith("/") => MovedPermanently("/training/" + string.subSequence(0, string.length() - 1))
      case _                              => showCmsContent(request.user, filename)
    }
  }

  private def showCmsContent(user: Option[User], filename: String): Result = {

    val key = s"cms/$filename/index.html";
    log.info(s"retrieving S3 key: $key")
    val urlOption: Option[URL] = S3BucketService.getCMS(key)
    log.info(s"Retrieving file at URL $urlOption")
    urlOption match {
      case Some(url) =>
        val string = scala.io.Source.fromURL(url).mkString
        Ok(views.html.training.training(user)(Html(string)))
      case None =>
        Ok(views.html.training.training(user)(Html(s"The following file was not found: <i>$filename</i>")))
    }
  }

}