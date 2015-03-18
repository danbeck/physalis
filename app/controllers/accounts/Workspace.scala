package controllers.accounts

import play.api.Logger
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
import play.api.data._
import play.api.data.Form
import models.Project
import java.util.UUID
import securesocial.core.utils._
import org.slf4j.LoggerFactory
import awscala.simpledb.SimpleDBClient
import awscala.simpledb.Domain
import awscala.simpledb.Item

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

  val projectForm = Form(
    tuple(
      "gitUrl" -> nonEmptyText(7, 200).verifying { text => text.matches(s"http://.*.git") },
      "appName" -> nonEmptyText(2, 70)))

  def newProjectPage = SecuredAction { implicit request =>
    Ok(views.html.workspace.index(request.user, Some(projectForm)))
  }

  def createNewProject = SecuredAction.async { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest("Oh no!: " + formWithErrors.errors)),
      tuple => {
        val project = Project(id = UUID.randomUUID().toString(),
          name = tuple._2,
          icon = "noicon",
          gitUrl = tuple._1,
          version = "1",
          user = request.user).save()

        val updatedUser = request.user.copy(projects = project :: request.user.projects)
        request.authenticator.updateUser(updatedUser).flatMap { authenticator =>
          Redirect(routes.Workspace.user(request.user.username.get)).touchingAuthenticator(authenticator)
        }
      })
  }
}