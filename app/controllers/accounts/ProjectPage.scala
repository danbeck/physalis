package controllers.accounts

import controllers.PhysalisSecureSocial
import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService
import models.Project
import views.html.workspace

class ProjectPage(override implicit val env: RuntimeEnvironment[User]) extends PhysalisSecureSocial {

  val USER_SERVICE = env.userService.asInstanceOf[UpdatableUserService]

  def show(username: String, projectname: String) = UserAwareAction { implicit request =>
    val project: Option[Project] = Project.findByUsernameAndProjectname(username, projectname)
    val shownUser = User.findByUsername(username)
    
    project match {
      case Some(proj) => Ok(workspace.project(request.user.orNull, shownUser.get, proj))
      case _ => Redirect(routes.ProjectPage.nonExistingProj())
    }

  }

  def nonExistingProj() = UserAwareAction { implicit request =>
//    Ok(views.workspace.nonExistingProject(request.user))
    Ok("")
  }
}