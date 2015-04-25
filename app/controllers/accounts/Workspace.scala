package controllers.accounts

import play.api.Logger
import play.api.mvc._
import play.api.data.validation.Constraints
import play.api.i18n.Messages
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
    def findUser(username: String) = User.findByUsername(username)
    def showMyAccount(user: User) = Future(Ok(workspace.myaccount(user)))

    def showPublicAccount(username: String) = Future.successful {
      findUser(username) match {
        case Some(user) => Ok(workspace.publicaccount(user))
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
    Ok(workspace.myaccount(request.user, Some(projectForm)))
  }

  def createNewProject = SecuredAction.async {
    implicit request =>
      projectForm.bindFromRequest.fold(
        formWithErrors => Future.successful(Redirect(routes.Workspace.newProjectPage())),
        validInputData => updateUser(validInputData._2, validInputData._1, request))
  }

  private def updateUser(projectname: String, gitUrl: String, request: SecuredRequest[AnyContent]) = {
    val project = Project(name = projectname,
      icon = None,
      gitUrl = gitUrl,
      userId = request.user.id,
      username = request.user.username.get).save()

    val updatedUser = request.user.copy(projects = project :: request.user.projects)
    request.authenticator.updateUser(updatedUser).flatMap { authenticator =>
      Redirect(routes.Workspace.user(request.user.username.get)).touchingAuthenticator(authenticator)
    }
  }
}
