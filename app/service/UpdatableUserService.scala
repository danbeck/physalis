package service

import securesocial.core.services.UserService
import models.User
import scala.concurrent.Await

trait UpdatableUserService extends UserService[User] {

  def update(user: User): User
}