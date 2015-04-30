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

class Build(override implicit val env: RuntimeEnvironment[User]) extends PhysalisSecureSocial {

  def build(username: String, projectname: String) = SecuredAction { implicit request =>
    val project: Option[Project] = Project.findByUsernameAndProjectname(username, projectname)

    (username, project) match {
      case (_, None) => Redirect(routes.ProjectPage.nonExistingProj())
      case (username, Some(_)) if username != request.user.usernameOption.get => Redirect(routes.ProjectPage.nonExistingProj())
      case (username, Some(proj)) => persistBuildRequest(request.user, proj)
    }
  }

  def persistBuildRequest(user: User, project: Project)(implicit request: SecuredRequest[AnyContent]) = {
    val buildTask = BuildTask(project = project, user = user, platform = "android")
    buildTask.save()
//    buildTask.gitClone()
//    buildTask.startBuilding()
    Ok(workspace.project(user, user, project))
  }
}
