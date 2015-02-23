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
import play.api.data.Forms._
import play.api.data.Form

class Workspace(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  val USER_SERVICE = env.userService.asInstanceOf[UpdatableUserService]

  def user(username: String) = UserAwareAction.async { implicit request =>
    def findUser(username: String) = USER_SERVICE.find(username)
    def showMyAccount(user: User) = Future { Ok(views.html.workspace.index(user)) }

    def showPublicAccount(username: String): Future[Result] = {
      val userFuture = findUser(username)
      userFuture.map {
        case Some(user) => Ok(views.html.workspace.index(user))
        case None       => Ok(views.html.workspace.notExistingUser(null))
      }
    }

    request.user match {
      case Some(u) if u.username.get == username => showMyAccount(u)
      case Some(u)                               => showPublicAccount(username)
      case None                                  => showPublicAccount(username)
    }
  }

  case class ProjectData(username: String, email: String, wantsNewsletter: Option[Boolean])

  val projectForm = Form(single("gitUrl" -> text))

  def newProjectPage = SecuredAction { implicit request =>
    Ok(views.html.workspace.newProject(request.user, projectForm))
  }

  def createNewProject = SecuredAction { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => BadRequest("Oh no!: " + formWithErrors.errors),
      value => Redirect(routes.Workspace.user(request.user.username.get)))
  }
}