package service

import securesocial.core.services.UserService
import models.User
import scala.concurrent.Await
import scala.concurrent.Future

trait UpdatableUserService extends UserService[User] {

  def find(username: String): Future[Option[User]]
  def update(user: User): User
}