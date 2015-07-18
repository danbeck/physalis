package controllers

import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService
import play.api.mvc.Action
import service.S3BucketService
import play.twirl.api.Html
import java.net.URL

class AmazonS3StaticController(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  def html(file: String) = UserAwareAction { implicit request =>
    val urlOption: Option[URL] = S3BucketService.getCMS(file)

    urlOption match {
      case Some(url) =>
        val string = scala.io.Source.fromURL(url).mkString
        Ok(views.html.training.training(request.user)(Html("dsfasd")))
      case None if request.user.isDefined =>
        Ok(views.html.training.training(request.user)(Html("Could not find key " + file)))
    }
  }
}