package controllers.workspace

import controllers.PhysalisSecureSocial
import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService
import models.Project
import views.html.workspace
import models.BuildTask
import play.Logger
import play.Play
import play.api.mvc.AnyContent

class ProjectPage(override implicit val env: RuntimeEnvironment[User]) extends PhysalisSecureSocial {
  val USER_SERVICE = env.userService.asInstanceOf[UpdatableUserService]

  def show(username: String, projectname: String) = UserAwareAction { implicit request =>
    val project: Option[Project] = Project.findByUsernameAndProjectname(username, projectname)
    val shownUser = User.findByUsername(username)
    val user = request.user

    (project, user) match {
      case (Some(proj), None) => Ok(workspace.project(null, false, proj))
      case (Some(proj), Some(u)) if u.id == shownUser.get.id => Ok(workspace.project(u, true, proj))
      case (Some(proj), Some(u)) => Ok(workspace.project(u, false, proj))
      case _ => Redirect(routes.ProjectPage.nonExistingProj())
    }
  }

  def nonExistingProj() = UserAwareAction { implicit request =>
    Ok(workspace.nonExistingProject(request.user.orNull))
  }
}