package controllers.accounts

import play.Logger
import play.api.mvc.{ Session, Result, Controller, Action, RequestHeader }
import play.api.data.validation.Constraints
import play.api.i18n.Messages
import play.api.mvc.Flash
import securesocial.core.java.UserAwareAction
import securesocial.core.RuntimeEnvironment
import models.User

class Index(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  def index = UserAwareAction { implicit request =>
    request.user match {
      case Some(user) => Ok(views.html.accounts.index(user))
      case None       => Ok(views.html.accounts.pleaseLogin())
    }
  }

  def user(username: String) = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) if u.main.fullName == username => Ok("Show me my private data")
      case Some(u)                  => Ok("Only show me public data")
      case _                        => Ok("No username given!")
    }
  }

  //  def logout = Action {
  //    Redirect("/logout").flashing("success" -> Messages("youve.been.logged.out"))
  //  }

  def login = Action { implicit request =>
    Ok(views.html.login(null))
  }
}