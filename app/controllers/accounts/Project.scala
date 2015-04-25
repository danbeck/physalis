package controllers.accounts

import controllers.PhysalisSecureSocial
import models.User
import securesocial.core.RuntimeEnvironment
import service.UpdatableUserService

class Project(override implicit val env: RuntimeEnvironment[User]) extends PhysalisSecureSocial {

  val USER_SERVICE = env.userService.asInstanceOf[UpdatableUserService]

  def show(username: String, project: String) = UserAwareAction { implicit request =>
    Ok("Show project")
  }
}
