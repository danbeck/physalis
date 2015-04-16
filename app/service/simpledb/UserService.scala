package service
import play.api.Logger

import awscala.simpledb.SimpleDBClient
import awscala.simpledb.Domain
import models.User
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import securesocial.core.providers.MailToken
import securesocial.core.BasicProfile
import securesocial.core.PasswordInfo
import securesocial.core.services.SaveMode
import play.api.Logger
import java.util.UUID
import models.{ Project, User }
import awscala.simpledb.Item
import securesocial.core.BasicProfile
import models.PhysalisProfile

class UserService extends UpdatableUserService {
  val logger: Logger = Logger(this.getClass())

  def find(username: String): Future[Option[User]] = {
    logger.info(s"find '$username")
    null
  }

  def projects: List[Project] = {
    SimpleDBService.findProjects().toList
  }

  def update(user: User): User = {
    SimpleDBService.saveUser(user)
    user
  }

  // not needed
  def deleteExpiredTokens() = {}

  // not needed
  def deleteToken(uuid: String): Future[Option[MailToken]] = { null }

  def find(providerId: String, profileId: String): Future[Option[BasicProfile]] = Future.successful {
    logger.info(s"Find '$providerId' '$profileId")
    SimpleDBService.findPhysalisProfile(providerId, profileId).map { _.toBasicProfile() }
  }

  // not needed?
  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    logger.info(s"FindByEmailAndProvide '$providerId' '$email")
    null
  }

  //not needed
  def findToken(token: String): Future[Option[MailToken]] = { null }

  //not needed
  def link(current: User, to: BasicProfile): Future[User] = { null }

  //not needed
  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = { null }

  def save(profile: BasicProfile, mode: SaveMode): Future[User] = {
    Logger.info(s"save $profile")
    Future.successful(saveProfileAndSearchUser(profile, mode));
  }

  private def saveProfileAndSearchUser(p: BasicProfile, mode: SaveMode): User = {
    logger.info(s"saveProfileAndSearchUser $p")
    val physalisProfileOption = SimpleDBService.findPhysalisProfile(p.providerId, p.userId)

    physalisProfileOption match {
      case Some(profile) =>
        SimpleDBService.saveProfile(profile)
        SimpleDBService.findUser(profile).get

      case None =>
        val profile = PhysalisProfile.create(p)
        SimpleDBService.saveProfile(profile)
        Logger.info("saveEmptyUser")
        SimpleDBService.saveEmptyUser(profile)
    }
  }

  //not needed
  def saveToken(token: MailToken): Future[MailToken] = { null }

  // not needed
  def updatePasswordInfo(user: models.User, info: PasswordInfo): Future[Option[BasicProfile]] = { null }
}
