package controllers.accounts

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

    project match {
      case Some(proj) => Ok(workspace.project(request.user.orNull, shownUser.get, proj))
      case _          => Redirect(routes.ProjectPage.nonExistingProj())
    }
  }

  def build(username: String, projectname: String) = SecuredAction { implicit request =>
    val project: Option[Project] = Project.findByUsernameAndProjectname(username, projectname)

    (username, project) match {
      case (_, None) => Redirect(routes.ProjectPage.nonExistingProj())
      case (username, Some(_)) if username != request.user.usernameOption.get => Redirect(routes.ProjectPage.nonExistingProj())
      case (username, Some(proj)) => persistBuildRequest(request.user, proj)
    }
  }

  def persistBuildRequest(user: User, project: Project)(implicit request: SecuredRequest[AnyContent]) = {
    val buildTask = BuildTask(projectId = project.id,
      userId = project.userId,
      gitUrl = project.gitUrl,
      platform = "android")
    buildTask.gitClone()
    buildTask.startBuilding()
    Ok(workspace.project(user, user, project))
  }

  def nonExistingProj() = UserAwareAction { implicit request =>
    Ok(workspace.nonExistingProject(request.user.orNull))
  }
}