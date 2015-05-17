package service.simpledb

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
import service.UpdatableUserService

class UserService extends UpdatableUserService {
  val logger: Logger = Logger(this.getClass())

  def findUserByUsername(username: String): Future[Option[User]] = {
    logger.info(s"Find '$username")
    Future.successful { User.findByUsername(username) }
  }

  def projects: List[Project] = Repository.findProjects().toList

  def find(providerId: String, profileId: String): Future[Option[BasicProfile]] = Future.successful {
    logger.info(s"Find '$providerId' '$profileId")
    PhysalisProfile.find(providerId, profileId).map(_.basicProfile)
  }

  def save(profile: BasicProfile, mode: SaveMode): Future[User] = {
    logger.info(s"SecureSocial - save profile ${profile.providerId} ${profile.userId} $mode")
    Future.successful(saveProfileAndSearchUser(profile, mode));
  }

  private def saveProfileAndSearchUser(basicProfile: BasicProfile, mode: SaveMode): User = {
    PhysalisProfile.find(basicProfile) match {
      case Some(pyhsalisProfile) =>
        pyhsalisProfile.save()
        User.find(pyhsalisProfile).get
      case None =>
        val profile = PhysalisProfile.create(basicProfile).save()
        createEmptyUser(profile).save()
    }
  }

  private def createEmptyUser(p: PhysalisProfile) = User(id = p.userId,
    main = p, identities = List(p))

  // -----------------------------------------------------------------
  //not needed

  def saveToken(token: MailToken) = null
  def updatePasswordInfo(user: User, info: PasswordInfo): Future[Option[BasicProfile]] = null
  def findToken(token: String) = null
  def link(current: User, to: BasicProfile) = null
  def deleteExpiredTokens() = Unit
  def deleteToken(uuid: String): Future[Option[MailToken]] = null
  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = null
  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    logger.info(s"FindByEmailAndProvide '$providerId' '$email")
    null
  }

}
