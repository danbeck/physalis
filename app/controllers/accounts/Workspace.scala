package controllers.accounts

import play.api.Logger
import play.api.mvc.{Session, Result, Controller, Action, RequestHeader}
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
import controllers.PhysalisSecureSocial
import securesocial.core.BasicProfile
import securesocial.core.services.SaveMode
import views.html.workspace

class Workspace(override implicit val env: RuntimeEnvironment[User]) extends PhysalisSecureSocial {

  val USER_SERVICE = env.userService.asInstanceOf[UpdatableUserService]

  def project(username: String, project: String) = UserAwareAction { implicit request =>
    Ok("Show project")
  }

  def savedata() = UserAwareAction {
    implicit request =>
      val profile = BasicProfile(
        providerId = "github",
        userId = "karin",
        firstName = Some("Karin"),
        lastName = Some("Beck"),
        fullName = Some("Karin Beck"),
        email = Some("karin@freen.et"),
        avatarUrl = Some("www"),
        authMethod = null,
        oAuth1Info = None,
        oAuth2Info = None,
        passwordInfo = None)
      USER_SERVICE.save(profile, SaveMode.SignUp)
      Ok("alles ok")
  }

  def user(username: String) = PhysalisUserAwareAction.async { implicit request =>
    def findUser(username: String) = USER_SERVICE.findUserByUsername(username)
    def showMyAccount(user: User) = Future {
      Ok(workspace.index(user))
    }

    def showPublicAccount(username: String): Future[Result] = {
      val userFuture = findUser(username)
      userFuture.map {
        case Some(user) => Ok(workspace.index(user))
        case None => Ok(workspace.notExistingUser(null))
      }
    }

    request.user match {
      case Some(u) if u.username.get == username => showMyAccount(u)
      case Some(u) => showPublicAccount(username)
      case None => showPublicAccount(username)
    }
  }

  case class ProjectData(username: String, email: String, wantsNewsletter: Option[Boolean])

  val projectForm = Form(
    tuple(
      "gitUrl" -> nonEmptyText(7, 200).verifying { text => text.matches(s"https?://.*.git") },
      "appName" -> nonEmptyText(2, 70)))

  def newProjectPage = SecuredAction { implicit request =>
    Ok(workspace.index(request.user, Some(projectForm)))
  }

  def createNewProject = SecuredAction.async { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Redirect(routes.Workspace.newProjectPage())),
      tuple => {
        val project = Project(name = tuple._2,
          icon = None,
          gitUrl = tuple._1,
          username = request.user.username.get).save()

        val updatedUser = request.user.copy(projects = project :: request.user.projects)
        request.authenticator.updateUser(updatedUser).flatMap { authenticator =>
          Redirect(routes.Workspace.user(request.user.username.get)).touchingAuthenticator(authenticator)
        }
      })
  }
}