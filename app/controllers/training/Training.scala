package controllers.training

import models.User
import securesocial.core.RuntimeEnvironment
import play.api.mvc.Action

/**
 * @author daniel
 */
class Training(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {
  def ubuntuHelloWorld = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) => Ok(views.html.training.ubuntuHelloWorld(u))
      case None    => Ok(views.html.training.ubuntuHelloWorld(null))
    }
  }
}