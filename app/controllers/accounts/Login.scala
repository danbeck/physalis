package controllers.accounts

import securesocial.core.RuntimeEnvironment
import models.User
import play.api.mvc.Action
import play.api.i18n.Messages
import play.api.data.Forms._
import play.api.data.Form
import securesocial.core.services.UserService
import service.UpdatableUserService
import service.UpdatableUserService
import service.UpdatableUserService
import service.UpdatableUserService

/**
 * Login and Signup are controlled by the same workflow:
 * - if a user logs in for his first time ever in physalis, a new account is created.
 * - If a user tries to signup with an actually existing account, he is logged in.
 */
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

  case class UserData(username: String, email: String, wantsNewsletter: Option[Boolean])

  val userForm = Form(mapping("username" -> text,
    "email" -> email,
    "wantNewsletter" -> optional(boolean))(UserData.apply)(UserData.unapply))

  def showEnterUserDataForm = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) => Ok(views.html.accounts.signupEnterUserData(u, userForm))
      case None    => Ok("Not logged in")
    }
  }

  def postUserData = UserAwareAction {
    implicit request =>
      request.user match {
        case Some(u) =>
          userForm.bindFromRequest.fold(
            formWithErrors => BadRequest("Oh no!: " + formWithErrors.errors),
            value => {
              updateUser(value, u)
              Redirect(routes.Index.user(value.username))
//              request.user = None;
//              request.
            })

        case None => Redirect(controllers.accounts.routes.Login.login())
      }
  }

  private def updateUser(data: UserData, user: User) = {
    val userservice: UpdatableUserService = env.userService.asInstanceOf[UpdatableUserService]
    val newuser = user.copy(email = Some(data.email), username = Some(data.username))
    userservice.update(user)
  }

}