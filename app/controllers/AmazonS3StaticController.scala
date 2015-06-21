package controllers

import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService
import play.api.mvc.Action

class AmazonS3StaticController(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  def html(file: String) = UserAwareAction { implicit request =>
    Ok("Here will come the amazon S3 static file controller")
    //    var f = new File(file)
    //
    //    if (f.exists())
    //      Ok(scala.io.Source.fromFile(f.getCanonicalPath()).mkString).as("text/html");
    //    else
    //      NotFound
  }
}