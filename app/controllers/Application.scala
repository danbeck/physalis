package controllers

import play.Logger
import play.api.mvc.{ Session, Result, Controller, Action, RequestHeader }
import play.api.data.validation.Constraints
import play.api.i18n.Messages
import play.api.mvc.Flash
import securesocial.core.java.UserAwareAction
import securesocial.core.RuntimeEnvironment
import models.DemoUser

class Application(override implicit val env: RuntimeEnvironment[DemoUser]) extends securesocial.core.SecureSocial[DemoUser] {

  //  def userAwareURL = UserAwareAction { implicit request =>
  //    val userName = request.user match {
  //      case Some(user) => "email:" + user.main.email
  //      case _          => "guest"
  //    }
  //    Ok("Hello %s".format(userName))
  //  }

  def loginRedirectURI = UserAwareAction { implicit request =>
    request.user match{
      case Some(u) => Redirect(accounts.routes.Index.user(u.main.userId))
      case None => BadRequest(views.html.index(null))
    }
  }
  
  
  def index = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) => Ok(views.html.index(u))
      case None    => Ok(views.html.index(null))
    }
  }

  def signup = UserAwareAction { implicit request =>
    Ok(views.html.login(null))
  }

  def logout = Action {
    Redirect(routes.CustomLoginController.logout).flashing("success" -> Messages("youve.been.logged.out"))
  }

  def login = Action { implicit request =>
    Ok(views.html.login(null))
  }
}