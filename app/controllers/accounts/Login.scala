package controllers.accounts

import securesocial.core.RuntimeEnvironment
import models.User
import play.api.mvc.Action
import play.api.i18n.Messages

class Login(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {
  def login = Action { implicit request =>
    Ok(views.html.login(null))
  }
  
  def loginRedirectURI = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) if !u.newUser => Redirect(routes.Index.user(u.username.get))
      case Some(u) if u.newUser  => Redirect(routes.Login.showEnterUserDataForm)
      case None                  => Redirect(controllers.routes.Application.index)
    }
  }

  def logout = Action {
    Redirect(controllers.routes.CustomLoginController.logout).flashing("success" -> Messages("youve.been.logged.out"))
  }

  
  def signup = UserAwareAction { implicit request =>
    Ok(views.html.accounts.signup(null))
  }

  def signupRedirectURI = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) if u.newUser  => Redirect(routes.Login.showEnterUserDataForm)
      case Some(u) if !u.newUser => Redirect(routes.Index.user(u.username.get))
      case None                  => Redirect(controllers.routes.Application.index)
    }
  }

  def showEnterUserDataForm = UserAwareAction { implicit request =>
    request.user match {
      // case Some(u) => Ok(views.html.signupEnterUserData(u,))
      case Some(u) => Ok("This is the first time you log in. Please enter your contact information")
      case None    => Ok("None")
    }
  }

  def postUserData = UserAwareAction {
    implicit request =>
      System.out.println("posted data!")
      Ok("Data posted")
  }
}