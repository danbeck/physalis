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
import models.Project
import awscala.simpledb.Item
import securesocial.core.BasicProfile

class SimpleDBUserService extends UpdatableUserService {

  //  def save(project: Project) = {
  //   
  //  }

  def find(username: String): Future[Option[User]] = {
    Logger.info(s"find '$username")
    null
  }

  def projects: List[models.Project] = { List() }

  def update(user: models.User): models.User = { null }

  def deleteExpiredTokens(): Unit = {}

  def deleteToken(uuid: String): Future[Option[MailToken]] = { null }

  def find(providerId: String, profileId: String): Future[Option[BasicProfile]] = Future.successful {
    SimpleDBService.findProfile(providerId, profileId)
  }

  def findUser(providerId: String, profileId: String) = {
  null
//  val itemOption = basicProfile_User_Domain.select(
//      s"select userId from BasicProfile_User where providerId = '${providerId}' and profileId = '${profileId}'").headOption
//
//    val userId = itemOption.get.attributes(0).value

  }

  //  def findUser(userId: String): User = {
  //    val itemOption = users_Domain.select(
  //      s"select fullname, email, wantsnewsletter from User where userId = '${userId}'").headOption
  //
  ////      case class User(id: String,
  ////                username: Option[String] = None,
  ////                fullname: Option[String] = None,
  ////                email: Option[String] = None,
  ////                wantNewsletter: Boolean = false,
  ////                projects: List[Project] = List(),
  ////                main: BasicProfile,
  ////                identities: List[BasicProfile]) {
  ////      
  //    }
  //      itemOption match {
  //      case Some(item) => User(id= userId,
  //          username = Some(item.attributes(0).value), 
  //          fullname = Some(item.attributes(1).value),
  //          email = Some(item.attributes(2).value),
  //          wantNewsletter = false,
  //          projects= List(),
  //          main= null,
  //          identities = null)
  //          item.attributes(0).value
  //    }
  //    
  //  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.info(s"findByEmail '$providerId' '$email")
    null
  }

  def findToken(token: String): Future[Option[MailToken]] = { null }

  def link(current: User, to: BasicProfile): Future[User] = { null }

  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = { null }

  def save(profile: BasicProfile, mode: SaveMode): Future[models.User] = {
    Future.successful {
      val foundProfile = SimpleDBService.findUserIdByProfile(profile)

      foundProfile match {
        case Some(id) =>
          SimpleDBService.updateProfile(id, profile)
          SimpleDBService.findUser(id)

        case None =>
          val id = SimpleDBService.saveProfile(profile)
          SimpleDBService.createUser(id, profile)
      }
    }
  }

  def saveToken(token: MailToken): Future[MailToken] = { null }

  def updatePasswordInfo(user: models.User, info: PasswordInfo): Future[Option[BasicProfile]] = { null }
}