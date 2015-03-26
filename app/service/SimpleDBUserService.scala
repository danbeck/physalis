package service

import play.api.Logger
import awscala.simpledb.SimpleDBClient
import awscala.simpledb.Domain
import models.User
import scala.concurrent.Future
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

  implicit val simpleDB = new SimpleDBClient()
  val usersDomain = simpleDB.createDomain("user")
  val projectsDomain = simpleDB.createDomain("project")
  val basicProfileDomain = simpleDB.createDomain("basicProfile")

  def save(project: Project) = {
    Logger.info(s"SimpleDB: Adding project '${project.id}' '${project.name}' '${project.gitUrl}'")
    projectsDomain.put(project.id,
      "gitUrl" -> project.gitUrl,
      "user" -> project.user.username.get,
      "project" -> project.name,
      "state" -> "NEW",
      "s3Url" -> "")

  }

  def find(username: String): Future[Option[User]] = {
    Logger.info(s"find '$username")
    null
  }

  def projects: List[models.Project] = { List() }

  def update(user: models.User): models.User = { null }

  def deleteExpiredTokens(): Unit = {}

  def deleteToken(uuid: String): Future[Option[MailToken]] = { null }

  def find(providerId: String, profileId: String): Future[Option[BasicProfile]] = {
    Logger.info(s"find '$providerId' '$profileId")

    //    val items: Seq[Item] = basicProfileDomain.select(s"select * from basicProfile where providerId = ${providerId} and profileId = ${profileId}")
    val items: Seq[Item] = basicProfileDomain.select(s"select id, providerId, profileId, userId from basicProfile where providerId = '${providerId}' and profileId = '${profileId}'")
    items.headOption match {
      case Some(item) =>
        val providerId = item.attributes(0).value
        val profileId = item.attributes(1).value
        Logger.info(s"found: providerId: ${providerId}, profileId: ${profileId}")
        val profile = BasicProfile(providerId = providerId,
          userId = profileId,
          firstName = None,
          lastName = None,
          fullName = None,
          email = None,
          avatarUrl = None,
          authMethod = null,
          oAuth1Info = None,
          oAuth2Info = None,
          passwordInfo = None)

        Future.successful(Some(profile))
      case None => Future.successful(None)
    }
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.info(s"findByEmail '$providerId' '$email")
    null
  }

  def findToken(token: String): Future[Option[MailToken]] = { null }

  def link(current: User, to: BasicProfile): Future[User] = { null }

  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = { null }

  def save(profile: BasicProfile, mode: SaveMode): Future[models.User] = {
    val newUser = User(id = UUID.randomUUID().toString, main = profile, identities = List(profile))

    val profileid = UUID.randomUUID().toString()
    basicProfileDomain.put(profileid,
      "providerId" -> profile.providerId,
      "profileId" -> profile.userId,
      "firstName" -> profile.firstName.getOrElse(null),
      "lastName" -> profile.lastName.getOrElse(null),
      "email" -> profile.email.getOrElse(null),
      "avatarUrl" -> profile.avatarUrl.getOrElse(null))

    find(profile.providerId, profile.userId)
    Future.successful(newUser)
  }

  def saveToken(token: MailToken): Future[MailToken] = { null }

  def updatePasswordInfo(user: models.User, info: PasswordInfo): Future[Option[BasicProfile]] = { null }
}