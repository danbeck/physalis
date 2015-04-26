package controllers

import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService

class Application(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  val userService = env.userService.asInstanceOf[UpdatableUserService]

  def index = UserAwareAction { implicit request =>
    def redirectToWorkspace(username:String) = Redirect(controllers.accounts.routes.Workspace.user(username))
    def showHomepage() = Ok(views.html.index())
        
    request.user match {
      case Some(u) => redirectToWorkspace(u.usernameOption.get)
      case None    => showHomepage()
    }
  }

  def projects = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) => Ok(views.html.projects(u, userService.projects))
      case None    => Ok(views.html.projects(null, userService.projects))
    }
  }

  def training = UserAwareAction { implicit request =>
    Ok("Here comes the training center")
  }

}