package service.simpledb

import play.api.Logger
import awscala.simpledb.SimpleDBClient
import models._
import awscala.simpledb.Item
import securesocial.core.BasicProfile
import scala.concurrent.Future
import java.util.UUID
import awscala.simpledb.Domain
import scala.collection.mutable.ArrayBuffer

object Repository {
  val logger: Logger = Logger(this.getClass())

  implicit val simpleDB = new SimpleDBClient()
  private val userDomain: Domain = simpleDB.createDomain("User")
  private val projectsDomain: Domain = simpleDB.createDomain("Project")
  private val profileDomain: Domain = simpleDB.createDomain("Profile")
  private val buildTasks: Domain = simpleDB.createDomain("BuildTask")

  def findNewBuildTasks(): Seq[BuildTask] = {

    val items = buildTasks.select(s"select * from BuildTasks where state = 'NEW'")
    //    val items = buildTasks.select(s"select * from BuildTasks where state = 'NEW'")

    items.map(item => {
      BuildTask(id = item.name,
        projectId = attrValue(item,"projectId"),
        userId = attrValue(item,"userId"),
        gitUrl = attrValue(item, "gitUrl"),
        platform = attrValue(item,"platform"))
    })
  }


  def saveProject(project: Project) = {
    logger.info(s"Save project '${project}")

    val projectData = ArrayBuffer(
      "name" -> project.name,
      "gitUrl" -> project.gitUrl,
      "userId" -> project.userId,
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
    userDomain.replaceIfExists(user.id, userData: _*)
  }


  def findUserByUsername(username: String): Option[User] = {
    val awsUser = userDomain.select( s"""select username,
      fullname,
      email,
      wantsnewsletter
      from User
      where username = '${username}'""").headOption

    awsUser.map(_.name) match {
      case Some(itemId) => findUser(itemId)
      case None => None
    }
  }

  def findProjects(): Seq[Project] = {
    val projectsItems = projectsDomain.select(s"select userId, name, icon, gitUrl, username from Project")
    projectsItems.map(project _)
  }

  def findUser(profile: PhysalisProfile): Option[User] = findUser(profile.userId)

  private def findUser(userId: String): Option[User] = {
    val profiles = findProfiles(userId)
    val projects = findProjects(userId)
    val awsItems = userDomain.select( s"""select username,
      fullname,
      email,
      wantsnewsletter
      from User
      where itemName() = '${userId}'""").headOption

    awsItems.map(user(_, projects, profiles(0), profiles))
  }


  private def findProjects(profile: PhysalisProfile): Seq[Project] = findProjects(profile.userId)

  private def findProjects(userId: String): Seq[Project] = {
    val projectsItems = projectsDomain.select(
      s"""select
        name,
        icon,
        gitUrl,
        userId,
        username
        from Project
        where userId = '${userId}'""")
    val projects = projectsItems.map(project _)
    projects
  }

  private def findProfiles(profile: PhysalisProfile): Seq[PhysalisProfile] = findProfiles(profile.userId)

  private def findProfiles(userId: String): Seq[PhysalisProfile] = {
    val profilesItems = profileDomain.select( s"""select
      providerId,
      providerUserId,
      firstName,
      lastName,
      fullName,
      email,
      avatarUrl,
      userId
          from Profile
          where userId = '${userId}'""")
    profilesItems.map(physalisProfile _)
  }

  def findBasicProfile(providerId: String, providerUserId: String): Option[PhysalisProfile] = {
    val profileOption = profileDomain.select( s"""select
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

    profileOption.map(physalisProfile _)
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

    itemOption.map(physalisProfile _)
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

  private def buildTask(item: Item) = {
    BuildTask(id = item.name,
      projectId = attrValue(item, "projectId"),
      userId = attrValue(item, "userId"),
      gitUrl = attrValue(item, "gitUrl"),
      platform = attrValue(item, "platform"))
  }

  private def attrValue(item: Item, attrName: String): String = {
    attrOption(item, attrName).get
  }

  private def attrOption(item: Item, attributeName: String): Option[String] = {
    val attr = item.attributes.find(_.name == attributeName)
    attr.map(value => value.getValue)
  }


  private def physalisProfile(item: Item) = {
    val id = item.name
    val providerId = attrValue(item, "providerId")
    val providerUserId = attrValue(item, "providerUserId")
    val firstName = attrOption(item, "firstName")
    val lastName = attrOption(item, "lastName")
    val fullName = attrOption(item, "fullName")
    val email = attrOption(item, "email")
    val avatarUrl = attrOption(item, "avatarUrl")
    val userId = attrValue(item, "userId")

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
    val userId = attrValue(item, "userId")
    val name = attrValue(item, "name")
    val icon = attrOption(item, "icon")
    val gitUrl = attrValue(item, "gitUrl")
    val username = attrValue(item, "username")

    Project(id = item.name,
      name = name,
      icon = icon,
      gitUrl = gitUrl,
      userId = userId,
      username = username)
  }

  private def user(item: Item, projects: Seq[Project], main: PhysalisProfile, identities: Seq[PhysalisProfile]): User = {
    user(item, projects.toList, main, identities.toList)
  }

  private def user(item: Item, projects: List[Project], main: PhysalisProfile, identities: List[PhysalisProfile]): User = {
    val username = attrOption(item, "username")
    val fullname = attrOption(item, "fullname")
    val email = attrOption(item, "email")
    val wantsnewsletter = attrOption(item, "wantsNewsletter") == Some("true")

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
