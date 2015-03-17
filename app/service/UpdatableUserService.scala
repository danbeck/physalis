package service

import securesocial.core.services.UserService
import models.User
import scala.concurrent.Await
import scala.concurrent.Future
import models.Project

trait UpdatableUserService extends UserService[User] {

  def find(username: String): Future[Option[User]]
  def update(user: User): User
  def projects: List[Project]
}