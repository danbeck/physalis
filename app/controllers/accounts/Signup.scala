package controllers.accounts

import securesocial.core.RuntimeEnvironment
import models.User

class Signup(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  def signup = UserAwareAction { implicit request =>
    Ok(views.html.accounts.signup(null))
  }

  def showEnterUserDataForm = UserAwareAction { implicit request =>
    request.user match {
      // case Some(u) => Ok(views.html.signupEnterUserData(u,))
      case Some(u) => Ok("")
      case None => Ok("None")
    }
  }

  def postUserData = UserAwareAction {
    implicit request =>
      System.out.println("posted data!")
      Ok("Data posted")
  }

}