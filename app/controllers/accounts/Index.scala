package controllers.accounts

import play.Logger
import play.api.mvc.{ Session, Result, Controller, Action, RequestHeader }
import play.api.data.validation.Constraints
import play.api.i18n.Messages
import play.api.mvc.Flash
import securesocial.core.java.UserAwareAction
import securesocial.core.RuntimeEnvironment
import models.User
import service.InMemoryUserService
import service.UpdatableUserService
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure

class Index(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  def index = UserAwareAction { implicit request =>
    request.user match {
      case Some(user) => Ok(views.html.accounts.index(user))
      case None       => Ok(views.html.accounts.pleaseLogin())
    }
  }

  def findUser(username: String): Future[Option[User]] = {
    env.userService.asInstanceOf[UpdatableUserService].find(username)
  }

  def user(username: String) = UserAwareAction.async { implicit request =>
    request.user match { //
      case Some(u) if u.username.get == username => Future { Ok(views.html.accounts.index(u)) }
      case _ => {
        val futureUser = findUser(username)
        futureUser.map {
          case Some(user) => Ok(views.html.accounts.index(user))
          case None       => Ok("This user doesn't exist")
        }
      }
    }
  }

  //  def logout = Action {
  //    Redirect("/logout").flashing("success" -> Messages("youve.been.logged.out"))
  //  }

  def login = Action { implicit request =>
    Ok(views.html.login(null))
  }
}