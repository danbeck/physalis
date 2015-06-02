package controllers.workspace

import play.api.Logger
import play.api.mvc._
import play.api.data.validation.Constraints
import play.api.i18n.Messages
import securesocial.core.java.UserAwareAction
import securesocial.core.RuntimeEnvironment
import models.User
import models.BuildTask
import models.BuildTask.validGitUrlPattern
import service.InMemoryUserService
import service.UpdatableUserService
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import play.api.data.Forms._
import play.api.data._
import play.api.data.Form
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
import java.net.{ URL, HttpURLConnection }
import java.io.IOException

class Workspace(override implicit val env: RuntimeEnvironment[User]) extends PhysalisSecureSocial {

  val USER_SERVICE = env.userService.asInstanceOf[UpdatableUserService]

  def user(username: String) = PhysalisUserAwareAction.async { implicit request =>
    def showMyAccount(user: User) = Future(Ok(workspace.myaccount(user)))
    def showPublicAccount(username: String) = Future.successful {
      User.findByUsername(username) match {
        case Some(user) => Ok(workspace.publicaccount(user))
        case None       => Ok(workspace.notExistingUser(null))
      }
    }

    request.user match {
      case Some(u) if !u.usernameOption.isDefined      => showPublicAccount(username)
      case Some(u) if u.usernameOption.get == username => showMyAccount(u)
      case Some(u)                                     => showPublicAccount(username)
      case None                                        => showPublicAccount(username)
    }
  }

  case class ProjectData(username: String, email: String, wantsNewsletter: Option[Boolean])

  val projectForm = Form(
    tuple(
      "gitUrl" -> nonEmptyText(7, 200).verifying { text => validGitUrlPattern.matcher(text).matches },
      "appName" -> nonEmptyText(2, 70)))

  def newProjectPage = SecuredAction { implicit request =>
    Ok(workspace.myaccount(request.user, Some(projectForm)))
  }

  def createNewProject = SecuredAction.async {
    implicit request =>
      projectForm.bindFromRequest.fold(
        formWithErrors => redirectAndFlashError("""The URL must end with ".git". """),
        validInputData => updateUserAndRedirect(validInputData._2, validInputData._1, request))
  }

  private def redirectAndFlashError(message: String) = {
    Future.successful(Redirect(routes.Workspace.newProjectPage())
      .flashing("error" -> message))
  }

  private def updateUserAndRedirect(projectname: String, gitUrl: String, request: SecuredRequest[AnyContent]): Future[Result] = {
    //    fileSize(new URL(gitUrl)) match {
    //      case Left(error)                   => redirectAndFlashError(error)
    //      case Right(i) if i >= 50000000 * 8 => redirectAndFlashError("""The .git file should not be bigger than 50 MB.""")
    //      case Right(i) =>
    //        val project = models.Project(name = projectname,
    //          icon = None,
    //          gitUrl = gitUrl,
    //          userId = request.user.id,
    //          username = request.user.usernameOption.get)
    //        project.save()
    //        updateUserAndRedirect(project, request)
    //    }
    val project = models.Project(name = projectname,
      icon = None,
      gitUrl = gitUrl,
      userId = request.user.id,
      username = request.user.usernameOption.get)
    project.save()
    updateUserAndRedirect(project, request)

  }

  private def updateUserAndRedirect(project: models.Project, request: SecuredRequest[AnyContent]): Future[Result] = {
    val updatedUser = request.user.copy(projects = project :: request.user.projects)
    request.authenticator.updateUser(updatedUser).flatMap { authenticator =>
      Redirect(routes.Workspace.user(request.user.usernameOption.get)).touchingAuthenticator(authenticator)
    }
  }

  private def fileSize(url: URL): Either[String, Long] = {
    var conn: HttpURLConnection = null
    try {
      conn = url.openConnection().asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("GET")
      conn.getInputStream()
      conn.getContentLengthLong match {
        case -1 => Left("Could not compute the size of the .git repository")
        case m  => Right(m)
      }
    } catch {
      case e: IOException => Left("Could not compute the size of the .git repository")
    } finally {
      conn.disconnect()
    }
  }
}
