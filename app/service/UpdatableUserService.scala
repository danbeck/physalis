package service

import securesocial.core.services.UserService
import models.User
import scala.concurrent.Await
import scala.concurrent.Future
import models.Project

trait UpdatableUserService extends UserService[User] {

  def findUserByUsername(username: String): Future[Option[User]]

  def projects: List[Project]
}