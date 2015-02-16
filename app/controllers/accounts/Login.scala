package controllers.accounts

import securesocial.core.RuntimeEnvironment
import models.User
import play.api.mvc.Action
import play.api.i18n.Messages

class Login(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {
  def loginRedirectURI = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) if !u.newUser => Redirect(routes.Index.user(u.username.get))
      case Some(u) if u.newUser  => Ok("This is a new user who must enter his username and password")
      //        env.userService.find(u.main, userId)
      //        Ok("")
      case None                  => BadRequest(views.html.index(null))
    }
  }

  def logout = Action {
    Redirect(controllers.routes.CustomLoginController.logout).flashing("success" -> Messages("youve.been.logged.out"))
  }

  def login = Action { implicit request =>
    Ok(views.html.login(null))
  }

}