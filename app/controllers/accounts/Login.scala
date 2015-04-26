package controllers.accounts

import securesocial.core.RuntimeEnvironment
import models.User
import play.api.mvc.Action
import play.api.i18n.Messages
import play.api.data.Forms._
import play.api.data.Form
import securesocial.core.services.UserService
import service.UpdatableUserService
import securesocial.core.utils._
import scala.concurrent.Future

/**
 * Login and Signup are controlled by the same workflow:
 * - if a user logs in for his first time ever in physalis, a new account is created.
 * - If a user tries to signup with an actually existing account, he is logged in.
 */
class Login(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

	private val userservice = env.userService.asInstanceOf[UpdatableUserService]

  def loginRedirectURI = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) if !u.newUser => Redirect(routes.Workspace.user(u.usernameOption.get))
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

  case class UserData(username: String, email: String, wantsNewsletter: Option[Boolean])

  val userForm = Form(mapping("username" -> text,
    "email" -> email,
    "wantNewsletter" -> optional(boolean))(UserData.apply)(UserData.unapply))

  def showEnterUserDataForm = SecuredAction { implicit request =>
    Ok(views.html.accounts.signupEnterUserData(request.user, userForm))
  }

  private def updateUser(data: UserData, user: User): User = {
    val newuser = user.copy(emailOption = Some(data.email), usernameOption = Some(data.username))
    userservice.update(newuser)
  }

  def postUserData = SecuredAction.async { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest("Oh no!: " + formWithErrors.errors)),
      value => {
        val updated = updateUser(value, request.user)
        request.authenticator.updateUser(updated).flatMap { authenticator =>
          Redirect(routes.Workspace.user(value.username)).touchingAuthenticator(authenticator)
        }
      })
  }

}