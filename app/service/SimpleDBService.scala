package service

import play.api.Logger
import awscala.simpledb.SimpleDBClient
import models.Project
import awscala.simpledb.Item
import securesocial.core.BasicProfile
import scala.concurrent.Future
import java.util.UUID
import models.User

object SimpleDBService {
  implicit val simpleDB = new SimpleDBClient()
  private val users = simpleDB.createDomain("User")
  private val projects = simpleDB.createDomain("Project")
  private val basicProfiles = simpleDB.createDomain("BasicProfile")
  private val buildTasks = simpleDB.createDomain("BuildTask")

  def saveProject(project: Project) = {
    Logger.info(s"SimpleDB: Adding project '${project.id}' '${project.name}' '${project.gitUrl}'")
    projects.put(project.id,
      "gitUrl" -> project.gitUrl,
      "user" -> project.user.username.get,
      "project" -> project.name,
      "state" -> "NEW",
      "s3Url" -> "")
  }

  def createUser(basicProfileId: String, basicProfile: BasicProfile) {
    val newUserId = UUID.randomUUID().toString()
    basicProfiles.put(basicProfileId, "userId" -> newUserId)
    User(id = newUserId,
      username = None,
      fullname = None,
      email = None,
      wantNewsletter = false,
      projects = List(),
      main = basicProfile,
      identities = List(basicProfile))
  }

  def findUser(basicProfileId: String): User = {
    val profileItem = basicProfiles.select(s"select userId from BasicProfile where itemName() = '${basicProfileId}'").head
    val mainProfile = basicProfile(profileItem)

    val userId = profileItem.attributes(0).value

    val allbasicProfilesItems = basicProfiles.select(s"select userId from BasicProfile where userId = '${userId}'")
    val basicProfiles = allbasicProfilesItems.map { item => basicProfile(item) }

    val userItem = users.select(s"select username, fullname, email, wantsnewsletter from User where itemName() = '${userId}'").head

    User(id = userId,
      username = Some(userItem.attributes(0).value),
      fullname = Some(userItem.attributes(1).value),
      email = Some(userItem.attributes(2).value),
      //      wantNewsletter = userItem.attributes(3).value,
      //      projects = List(),
      main = mainProfile,
      identities = basicProfiles)

  }

  def findProfile(profile: BasicProfile): Future[Option[String]] = {
    Logger.info(s"find '${profile.providerId}' '${profile.userId}")

    val items = basicProfiles.select(
      s"""select id, providerId, profileId, userId from BasicProfile where 
        providerId = '${profile.providerId}' and profileId = '${profile.userId}'""")

    items.headOption match {
      case Some(item) => Future.successful(Some(item.attributes(0).value))
      case None       => Future.successful(None)
    }
  }

  def updateProfile(id: String, profile: BasicProfile) = {
    basicProfiles.put(id,
      "providerId" -> profile.providerId,
      "profileId" -> profile.userId,
      "firstName" -> profile.firstName.getOrElse(null),
      "lastName" -> profile.lastName.getOrElse(null),
      "email" -> profile.email.getOrElse(null),
      "avatarUrl" -> profile.avatarUrl.getOrElse(null))
  }

  def saveProfile(profile: BasicProfile) = {
    val id = UUID.randomUUID().toString();
    basicProfiles.put(id,
      "providerId" -> profile.providerId,
      "profileId" -> profile.userId,
      "firstName" -> profile.firstName.getOrElse(null),
      "lastName" -> profile.lastName.getOrElse(null),
      "email" -> profile.email.getOrElse(null),
      "avatarUrl" -> profile.avatarUrl.getOrElse(null))
    id
  }

  private def basicProfile(item: Item) = {
    val providerId = item.attributes(0).value
    val profileId = item.attributes(1).value
    BasicProfile(providerId = providerId,
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

  }

}