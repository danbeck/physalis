package service

import awscala.simpledb.SimpleDBClient
import awscala.simpledb.Domain
import models.User
import scala.concurrent.Future
import securesocial.core.providers.MailToken
import securesocial.core.BasicProfile
import securesocial.core.PasswordInfo
import securesocial.core.services.SaveMode

class SimpleDBUserService extends UpdatableUserService {

  implicit val simpleDB = new SimpleDBClient()
  val domain: Domain = simpleDB.createDomain("buildTasks")

  // Members declared in service.UpdatableUserService
  def find(username: String): Future[Option[User]] = { null }
  def projects: List[models.Project] = { List() }
  def update(user: models.User): models.User = { null }

  def deleteExpiredTokens(): Unit = {}
  def deleteToken(uuid: String): Future[Option[MailToken]] = { null }
  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = { null }
  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = { null }
  def findToken(token: String): Future[Option[MailToken]] = { null }
  def link(current: User, to: BasicProfile): Future[User] = { null }
  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = { null }
  def save(profile: BasicProfile, mode: SaveMode): Future[models.User] = { null }
  def saveToken(token: MailToken): Future[MailToken] = { null }
  def updatePasswordInfo(user: models.User, info: PasswordInfo): Future[Option[BasicProfile]] = { null }
}