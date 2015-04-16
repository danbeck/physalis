package service.simpledb

import play.api.Logger
import awscala.simpledb.SimpleDBClient
import models.Project
import awscala.simpledb.Item
import securesocial.core.BasicProfile
import scala.concurrent.Future
import java.util.UUID
import models.User
import awscala.simpledb.Domain
import models.PhysalisProfile
import models.Project
import scala.collection.mutable.ArrayBuffer

object Repository {
  val logger: Logger = Logger(this.getClass())

  implicit val simpleDB = new SimpleDBClient()
  private val userDomain: Domain = simpleDB.createDomain("User")
  private val projectsDomain: Domain = simpleDB.createDomain("Project")
  private val profileDomain: Domain = simpleDB.createDomain("Profile")
  private val buildTasks: Domain = simpleDB.createDomain("BuildTask")

  def saveProject(project: Project) = {
    logger.info(s"Save project '${project}")

    val projectData = ArrayBuffer(
      "name" -> project.name,
      "gitUrl" -> project.gitUrl,
      "username" -> project.username)

    if (project.icon.isDefined) projectData += ("icon" -> project.icon.get)
    projectsDomain.put(project.id, projectData: _*)
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

    logger.info(s"Save user ${user}")

    val userData = ArrayBuffer("wantsnewsletter" -> user.wantNewsletter.toString())

    if (user.username.isDefined) userData += "username" -> user.username.get
    if (user.fullname.isDefined) userData += "fullname" -> user.fullname.get
    if (user.email.isDefined) userData += "email" -> user.email.get

    userDomain.put(user.id, userData: _*)

    user
  }

  def saveUser(user: User) = {

    val userData = ArrayBuffer("wantsnewsletter" -> user.wantNewsletter.toString())
    if (user.username.isDefined) userData += "username" -> user.username.get
    if (user.fullname.isDefined) userData += "fullname" -> user.fullname.get
    if (user.email.isDefined) userData += "fullname" -> user.email.get
    userDomain.put(user.id, userData: _*)
  }

  def findProjects(): Seq[Project] = {
    val projectsItems = projectsDomain.select(s"select userId, name, icon, gitUrl from Projects")
    projectsItems.map(project(_))
  }

  def findUser(profile: PhysalisProfile): Option[User] = {
    val profilesItems = profileDomain.select(s"""select 
      providerId, 
      providerUserId, 
      firstName, 
      lastName, 
      fullName, 
      email, 
      avatarUrl, 
      userId 
          from Profile 
          where userId = '${profile.userId}'""")
    val profiles = profilesItems.map { item => physalisProfile(item) }

    val projectsItems = projectsDomain.select(s"""select 
      name, 
      icon, 
      gitUrl, 
      username 
      from Project 
      where userId = '${profile.userId}'""")
    val projects = projectsItems.map { item => project(item) }

    val userItem = userDomain.select(s"""select username, 
      fullname, 
      email, 
      wantsnewsletter 
      from User 
      where itemName() = '${profile.userId}'""").headOption

    userItem.map(user(_, projects, profile, profiles))
  }

  def findBasicProfile(providerId: String, providerUserId: String): Option[PhysalisProfile] = {
    val profileOption = profileDomain.select(s"""select 
      providerId, 
      providerUserId, 
      firstName, 
      lastName, 
      fullName, 
      email, 
      avatarUrl, 
      userId 
          from Profile 
          where providerId = '${providerId}', providerUserId= '${providerUserId}'""").headOption

    profileOption.map(physalisProfile(_))
  }

  def findPhysalisProfile(providerId: String, providerUserId: String): Option[PhysalisProfile] = {
    logger.info(s"Search Profile: '${providerId}/${providerUserId}'")
    val itemOption = profileDomain.select(
      s"""select 
            providerId, 
            providerUserId, 
            firstName, 
            lastName, 
            fullName, 
            email, 
            avatarUrl, 
            userId from Profile 
          where 
          providerId = '${providerId}' and providerUserId = '${providerUserId}'""").headOption

    itemOption.map(physalisProfile(_))
  }

  def findUserIdByProfile(profile: BasicProfile): Option[String] = {
    findUserIdByProfile(profile.providerId, profile.userId)
  }

  def findUserIdByProfile(providerId: String, providerUserId: String): Option[String] = {
    logger.info(s"Search user '${providerId}' '${providerUserId}")

    profileDomain.select(
      s"""select userId from Profile 
          where providerId = '${providerId}' and providerUserId = '${providerUserId}'""")
      .headOption.map(_.attributes(0).value)
  }

  def saveProfile(p: PhysalisProfile) = {
    logger.info(s"Save profile ${p}")

    val profileData = ArrayBuffer(
      "providerId" -> p.providerId,
      "providerUserId" -> p.providerUserId,
      "userId" -> p.userId)

    if (p.firstName.isDefined) profileData += "firstName" -> p.firstName.get
    if (p.lastName.isDefined) profileData += "lastName" -> p.lastName.get
    if (p.fullName.isDefined) profileData += "fullName" -> p.fullName.get
    if (p.email.isDefined) profileData += "email" -> p.email.get
    if (p.avatarUrl.isDefined) profileData += "avatarUrl" -> p.avatarUrl.get

    profileDomain.put(p.id, profileData: _*)
    Logger.info(s"saved profile: ${p.id}")
  }

  private def physalisProfile(item: Item) = {
    val id = item.name
    val providerId = item.attributes.find(_.name == "providerId").get.value
    val providerUserId = item.attributes.find(_.name == "providerUserId").get.value
    val firstName = item.attributes.find(_.name == "firstName").map(_.value)
    val lastName = item.attributes.find(_.name == "lastName").map(_.value)
    val fullName = item.attributes.find(_.name == "fullName").map(_.value)
    val email = item.attributes.find(_.name == "email").map(_.value)
    val avatarUrl = item.attributes.find(_.name == "avatarUrl").map(_.value)
    val userId = item.attributes.find(_.name == "userId").get.value

    PhysalisProfile(id = id,
      providerId = providerId,
      providerUserId = providerUserId,
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      email = fullName,
      avatarUrl = avatarUrl,
      userId = userId)
  }

  private def project(item: Item) = {
    val userId = item.attributes.collectFirst { case attr if attr.name == "userId" => attr.value }.get
    val name = item.attributes.collectFirst { case attr if attr.name == "name" => attr.value }.get
    val icon = item.attributes.collectFirst { case attr if attr.name == "icon" => attr.value }
    val gitUrl = item.attributes.collectFirst { case attr if attr.name == "gitUrl" => attr.value }.get
    val username = item.attributes.collectFirst { case attr if attr.name == "username" => attr.value }.get

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
    val username = item.attributes.collectFirst { case attr if attr.name == "username" => attr.value }
    val fullname = item.attributes.collectFirst { case attr if attr.name == "fullname" => attr.value }
    val email = item.attributes.collectFirst { case attr if attr.name == "email" => attr.value }
    val wantsnewsletter = item.attributes.collectFirst { case attr if attr.name == "wantsNewsletter" => attr.value } == Some("true")

    User(id = item.name,
      username = username,
      fullname = fullname,
      email = email,
      wantNewsletter = wantsnewsletter,
      projects = projects,
      main = main,
      identities = identities)
  }
}
