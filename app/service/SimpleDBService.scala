package service

import play.api.Logger
import awscala.simpledb.SimpleDBClient
import models.Project
import awscala.simpledb.Item
import securesocial.core.BasicProfile
import scala.concurrent.Future
import java.util.UUID
import models.User
import awscala.simpledb.Domain
import securesocial.core.BasicProfile
import models.PhysalisProfile

object SimpleDBService {
  implicit val simpleDB = new SimpleDBClient()
  private val users: Domain = simpleDB.createDomain("User")
  private val projects: Domain = simpleDB.createDomain("Project")
  private val basicProfiles: Domain = simpleDB.createDomain("BasicProfile")
  private val buildTasks: Domain = simpleDB.createDomain("BuildTask")

  def saveProject(project: Project) = {
    Logger.info(s"SimpleDB: Adding project '${project.id}' '${project.name}' '${project.gitUrl}'")
    projects.put(project.id,
      "gitUrl" -> project.gitUrl,
      "user" -> project.user.username.get,
      "project" -> project.name,
      "state" -> "NEW",
      "s3Url" -> "")
  }

  def createUser(basicProfileId: String, basicProfile: BasicProfile): User = {
    val userId = UUID.randomUUID().toString()
    basicProfiles.put(basicProfileId, "userId" -> userId)
    User(id = userId,
      username = None,
      fullname = None,
      email = None,
      wantNewsletter = false,
      projects = List(),
      main = basicProfile,
      identities = List(basicProfile))
  }

  def findUser(basicProfileId: String): User = {
    val profile_Record = basicProfiles.select(s"select userId from BasicProfile where itemName() = '${basicProfileId}'").head
    val profile = basicProfile(profile_Record)

    val profiles_Record = basicProfiles.select(s"""select providerId, profileId, firstName, lastName, fullName, email, avatarUrl, userId 
          from BasicProfile where userId = ${profile.userId}""")
    val profiles = profiles_Record.map { item => basicProfile(item) }

    val projects_Record = projects.select(s"select itemName(), userId, name, icon, gitUrl from Projects where userId = ${profile.userId}")
    val userProjects = projects_Record.map { item => project(item) }

    val userRecord = users.select(s"select username, fullname, email, wantsnewsletter from User where itemName() = '${profile.userId}'").head
    user(item = userRecord, projects = userProjects.toList, main = profile, identities = profiles.toList)
  }

  def findBasicProfile(providerId: String, profileId: String): Option[BasicProfile] = {
    val profileOption = basicProfiles.select(s"""select itemName(), providerId, profileId, firstName, lastName, fullName, email, avatarUrl, userId 
          from BasicProfile where providerId = '${providerId}', profileid = '${profileId}'""").headOption

    profileOption match {
      case Some(profileRecord) => Some(basicProfile(profileRecord))
      case None                => None
    }
  }

  def findUserIdByProfile(profile: BasicProfile): Option[String] = {
    findUserIdByProfile(profile.providerId, profile.userId)
  }

  def findUserIdByProfile(providerId: String, userId: String): Option[String] = {
    Logger.info(s"find '${providerId}' '${userId}")

    val items = basicProfiles.select(
      s"""select itemName(), providerId, profileId, userId from BasicProfile where 
			  providerId = '${providerId}' and profileId = '${userId}'""")

    items.headOption match {
      case Some(item) => Some(item.attributes(0).value)
      case None       => None
    }
  }

  def findProfile(providerId: String, userId: String): Option[BasicProfile] = {
    Logger.info(s"find '${providerId}' '${userId}")

    val itemOption = basicProfiles.select(
      s"""select itemName(), providerId, profileId, userId from BasicProfile where 
        providerId = '${providerId}' and profileId = '${userId}'""").headOption

    itemOption match {
      case Some(item) => Some(basicProfile(item))
      case None       => None
    }
  }

  //  def updateProfile(id: String, profile: BasicProfile) = {
  //    val userId = basicProfiles.select(s"select userId from BasicProfile where itemName() = '${id}'").head.attributes(0).value
  //    saveProfileDataRow(id, profile, userId)
  //  }

  def saveProfile(profile: PhysalisProfile) = {
    basicProfiles.put(profile.id,
      "providerId" -> profile.providerId,
      "profileId" -> profile.profileId,
      "firstName" -> profile.firstName.orNull,
      "lastName" -> profile.lastName.orNull,
      "fullName" -> profile.fullName.orNull,
      "email" -> profile.email.orNull,
      "avatarUrl" -> profile.avatarUrl.orNull,
      "userId" -> profile.userId)
  }

  //  def saveProfile(profile: BasicProfile) = {
  //    val id = UUID.randomUUID().toString();
  //    saveProfileDataRow(id, profile)
  //  }

//  def saveProfileDataRow(id: String, profile: BasicProfile, userId: String = null) = {
//    basicProfiles.put(id,
//      "providerId" -> profile.providerId,
//      "profileId" -> profile.userId,
//      "firstName" -> profile.firstName.getOrElse(null),
//      "lastName" -> profile.lastName.getOrElse(null),
//      "fullName" -> profile.fullName.getOrElse(null),
//      "email" -> profile.email.getOrElse(null),
//      "avatarUrl" -> profile.avatarUrl.getOrElse(null),
//      "userId" -> userId)
//    id
//  }

  private def physalisProfile(item: Item) = {

    val id = item.name
    val providerId = item.attributes(0).value
    val profileId = item.attributes(1).value
    val firstName = item.attributes(2).value
    val lastName = item.attributes(3).value
    val fullName = item.attributes(4).value
    val email = item.attributes(5).value
    val avatarUrl = item.attributes(6).value
    val userId = item.attributes(7).value

    PhysalisProfile(id = id,
      providerId = providerId,
      profileId = profileId,
      firstName = Option(firstName),
      lastName = Option(lastName),
      fullName = Option(fullName),
      email = Option(fullName),
      avatarUrl = Option(avatarUrl),
      userId = userId)
  }

  private def project(item: Item) = {
    val userId = item.attributes(0).value
    val name = item.attributes(1).value
    val icon = item.attributes(2).value
    val gitUrl = item.attributes(3).value

    Project(id = item.name,
      name = name,
      icon = icon,
      gitUrl = gitUrl,
      user = null)
  }

  private def user(item: Item, projects: List[Project], main: BasicProfile, identities: List[BasicProfile]) = {
    val username = item.attributes(0).value
    val fullname = item.attributes(1).value
    val email = item.attributes(2).value
    val wantsnewsletter = item.attributes(3).value match {
      case "true" => true
      case _      => false
    }

    User(id = item.name,
      username = Option(username),
      fullname = Option(fullname),
      email = Option(email),
      wantNewsletter = wantsnewsletter,
      projects = projects,
      main = main,
      identities = identities)

  }
}