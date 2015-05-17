package controllers

import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService
import play.api.mvc.Action

class Application(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  val userService = env.userService.asInstanceOf[UpdatableUserService]

  def index = UserAwareAction { implicit request =>
    def redirectToWorkspace(username: String) = Redirect(controllers.workspace.routes.Workspace.user(username))
    def showHomepage() = Ok(views.html.index())
    def showHomepageForUser(user: User) = Ok(views.html.index(user))

    request.user match {
      case Some(u) if u.usernameOption.isDefined  => redirectToWorkspace(u.usernameOption.get)
      case Some(u) if !u.usernameOption.isDefined => showHomepageForUser(u)
      case _                                      => showHomepage()
    }
  }

  def projects = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) => Ok(views.html.projects(u, userService.projects))
      case None    => Ok(views.html.projects(null, userService.projects))
    }
  }

  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }

  def training = UserAwareAction { implicit request =>
    Ok("Here comes the training center")
  }

}