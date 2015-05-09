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
import models.Project
import play.api.libs.json.Json

class Build(override implicit val env: RuntimeEnvironment[User]) extends PhysalisSecureSocial {

  def build(username: String, projectname: String) = SecuredAction { implicit request =>
    val project: Option[Project] = Project.findByUsernameAndProjectname(username, projectname)

    (username, project) match {
      case (_, None) => Redirect(routes.ProjectPage.nonExistingProj())
      case (username, Some(_)) if username != request.user.usernameOption.get => Redirect(routes.ProjectPage.nonExistingProj())
      case (username, Some(proj)) => persistBuildRequest(request.user, proj)
    }
  }

  def latestBuildJson(username: String, projectname: String) = UserAwareAction { implicit request =>
    val project: Option[Project] = Project.findByUsernameAndProjectname(username, projectname)
    val shownUser = User.findByUsername(username)
    val projectNotFound = Ok(Json.obj("error" -> "project not found"))

    (shownUser, request.user, project) match {
      case (_, _, None) => projectNotFound
      case (_, None, Some(proj)) if !proj.visible => projectNotFound
      case (Some(u), Some(ru), Some(proj)) if !proj.visible && u.id != ru.id => projectNotFound
      case (Some(u), _, Some(proj)) => {
        val android = proj.lastBuildTask("android")
        val ubuntu = proj.lastBuildTask("ubuntu")
        Ok(Json.obj(
          "android" -> Json.obj(
            "state" -> android.map(_.state),
            "url" -> android.map(_.s3Url)),
          "ubuntu" -> Json.obj(
            "state" -> ubuntu.map(_.state),
            "url" -> ubuntu.map(_.s3Url))))
      }
    }
  }

  def persistBuildRequest(user: User, project: Project)(implicit request: SecuredRequest[AnyContent]) = {
    BuildTask(project = project, user = user, platform = "android").save()
    BuildTask(project = project, user = user, platform = "ubuntu").save()
    Redirect(controllers.workspace.routes.ProjectPage.show(user.usernameOption.get, project.name))
  }
}
