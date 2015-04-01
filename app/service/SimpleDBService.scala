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
import models.PhysalisProfile
import models.PhysalisProfile
import models.PhysalisProfile
import models.Project

object SimpleDBService {
  implicit val simpleDB = new SimpleDBClient()
  private val usersDomain: Domain = simpleDB.createDomain("User")
  private val projectsDomain: Domain = simpleDB.createDomain("Project")
  private val profileDomain: Domain = simpleDB.createDomain("BasicProfile")
  private val buildTasks: Domain = simpleDB.createDomain("BuildTask")

  def saveProject(project: Project) = {
    Logger.info(s"SimpleDB: Adding project '${project.id}' '${project.name}' '${project.gitUrl}'")
    projectsDomain.put(project.id,
      "name" -> project.name,
      "icon" -> project.icon,
      "gitUrl" -> project.gitUrl,
      "username" -> project.username)
  }

  def saveEmptyUser(profile: PhysalisProfile): User = {
    val user = User(id = profile.userId,
      username = None,
      fullname = None,
      email = None,
      wantNewsletter = false,
      projects = List(),
      main = profile,
      identities = List(profile))

    Logger.info(s"save user: ${user}")
    
    usersDomain.put(user.id,
      "username" -> user.username.orNull,
      "fullname" -> user.fullname.orNull,
      "email" -> user.email.orNull,
      "wantsnewsletter" -> user.wantNewsletter.toString())

    user
  }

  def saveUser(user: User) = {
    usersDomain.put(user.id,
      "username" -> user.username.orNull,
      "fullname" -> user.fullname.orNull,
      "email" -> user.email.orNull,
      "wantsnewsletter" -> user.wantNewsletter.toString())
  }
  def findProjects(): Seq[Project] = {
    val projectsItems = projectsDomain.select(s"select userId, name, icon, gitUrl from Projects")
    projectsItems.map(project(_))
  }

  def findUser(profile: PhysalisProfile): Option[User] = {
    val profilesItems = profileDomain.select(s"""select providerId, profileId, firstName, lastName, fullName, email, avatarUrl, userId 
          from BasicProfile where userId = ${profile.userId}""")
    val profiles = profilesItems.map { item => physalisProfile(item) }

    val projectsItems = projectsDomain.select(s"select name, icon, gitUrl, username from Projects where userId = ${profile.userId}")
    val projects = projectsItems.map { item => project(item) }

    val userItem = usersDomain.select(s"select username, fullname, email, wantsnewsletter from User where itemName() = '${profile.userId}'").headOption

    userItem.map(user(_, projects, profile, profiles))
  }

  def findBasicProfile(providerId: String, profileId: String): Option[PhysalisProfile] = {
    val profileOption = profileDomain.select(s"""select itemName(), providerId, profileId, firstName, lastName, fullName, email, avatarUrl, userId 
          from BasicProfile where providerId = '${providerId}', profileid = '${profileId}'""").headOption

    profileOption match {
      case Some(profileRecord) => Some(physalisProfile(profileRecord))
      case None                => None
    }
  }

  def findPhysalisProfile(providerId: String, profileId: String): Option[PhysalisProfile] = {
    Logger.info(s"search physalisProfile: '${providerId}/${profileId}'")
    val itemOption = profileDomain.select(
      s"""select 
            providerId, 
            profileId, 
            firstName, 
            lastName, 
            fullName, 
            email, 
            avatarUrl, 
            userId from BasicProfile 
          where 
          providerId = '${providerId}' and profileId = '${profileId}'""").headOption

    val result = itemOption.map(physalisProfile(_))
    Logger.info(s"found ${result}")
    result
  }

  def findUserIdByProfile(profile: BasicProfile): Option[String] = {
    findUserIdByProfile(profile.providerId, profile.userId)
  }

  def findUserIdByProfile(providerId: String, userId: String): Option[String] = {
    Logger.info(s"find '${providerId}' '${userId}")

    val items = profileDomain.select(
      s"""select itemName(), providerId, profileId, userId from BasicProfile where 
			  providerId = '${providerId}' and profileId = '${userId}'""")

    items.headOption match {
      case Some(item) => Some(item.attributes(0).value)
      case None       => None
    }
  }

  def saveProfile(profile: PhysalisProfile) = {
    Logger.info(s"save profile ${profile}")
    profileDomain.put(profile.id,
      "providerId" -> profile.providerId,
      "profileId" -> profile.profileId,
      "firstName" -> profile.firstName.orNull,
      "lastName" -> profile.lastName.orNull,
      "fullName" -> profile.fullName.orNull,
      "email" -> profile.email.orNull,
      "avatarUrl" -> profile.avatarUrl.orNull,
      "userId" -> profile.userId)
  }

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
    val username = item.attributes(4).value

    Project(id = item.name,
      name = name,
      icon = icon,
      gitUrl = gitUrl,
      username = username)
  }

  private def user(item: Item, projects: Seq[Project], main: PhysalisProfile, identities: Seq[PhysalisProfile]): User = {
    user(item, projects.toList, main, identities.toList)
  }

  private def user(item: Item, projects: List[Project], main: PhysalisProfile, identities: List[PhysalisProfile]): User = {
    val username = item.attributes(0).value
    val fullname = item.attributes(1).value
    val email = item.attributes(2).value
    val wantsnewsletter = item.attributes(3).value == "true"

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
