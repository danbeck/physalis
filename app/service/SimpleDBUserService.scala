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
import models.{Project,User}
import awscala.simpledb.Item
import securesocial.core.BasicProfile

class SimpleDBUserService extends UpdatableUserService {

  def find(username: String): Future[Option[User]] = {
    Logger.info(s"find '$username")
    null
  }

  // not needed
  def deleteExpiredTokens() = {}

  // not needed
  def deleteToken(uuid: String): Future[Option[MailToken]] = { null }

  def find(providerId: String, profileId: String): Future[Option[BasicProfile]] = Future.successful {
    SimpleDBService.findProfile(providerId, profileId)
  }


  // not needed?
  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.info(s"findByEmail '$providerId' '$email")
    null
  }

  //not needed
  def findToken(token: String): Future[Option[MailToken]] = { null }

  //not needed
  def link(current: User, to: BasicProfile): Future[User] = { null }

  //not needed
  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = { null }

  def save(profile: BasicProfile, mode: SaveMode): Future[User] = 
    Future.successful (saveProfileAndSearchUser(profile, mode))
    

  private def saveProfileAndSearchUser(profile:BasicProfile, mode: SaveMode):User = {
      val physalisProfileOption = SimpleDBService.findPhysalisProfile(profile.providerId, profile.profileId)

      physalisProfileOption match {
        case Some(physalisProfile) =>
            SimpleDBService.saveProfile(profile)
            SimpleDBService.findUser(profile)

        case None =>
            val physalisProfile = PhysalisProfile(providerId = profile.providerId,
                profileId = profileId, 
                firstName = profile.firstName,
                lastName = profile.lastName,
                fullname = profile.fullName,
                email = profile.email,
                avatarUrl = profile.avatarUrl,
                userId = profile.userId)

            SimpleDBService.saveProfile(profile)
            SimpleDBService.createEmptyUser(profile)
      }
  }

  //not needed
  def saveToken(token: MailToken): Future[MailToken] = { null }

  // not needed
  def updatePasswordInfo(user: models.User, info: PasswordInfo): Future[Option[BasicProfile]] = { null }
}
