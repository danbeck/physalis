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
    val items = buildTasks.select(s"select * from BuildTask where state = 'NEW'")

    items.map(item => {
      val projectId = attrValue(item, "projectId")
      val userId = attrValue(item, "userId")
      val platform = attrValue(item, "platform")
      val project = findProjectById(projectId).get
      val user = findUser(userId).get

      BuildTask(id = item.name,
        project = project,
        user = user,
        platform = platform)
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

  def saveUser(user: User): User = {
    val userData = ArrayBuffer("wantsnewsletter" -> user.wantsNewsletter.toString,
      "accountPlan" -> user.accountPlan)
    if (user.usernameOption.isDefined) userData += "username" -> user.usernameOption.get
    if (user.fullnameOption.isDefined) userData += "fullname" -> user.fullnameOption.get
    if (user.emailOption.isDefined) userData += "email" -> user.emailOption.get
    userDomain.replaceIfExists(user.id, userData: _*)
    user
  }

  def findUserByUsername(username: String): Option[User] = {
    val awsUser = userDomain.select(s"""select username,
      fullname,
      email,
      wantsnewsletter
      from User
      where username = '${username}'""").headOption
    awsUser.map(_.name) match {
      case Some(itemId) => findUser(itemId)
      case None         => None
    }
  }

  def findProjects(): Seq[Project] = {
    val projectsItems = projectsDomain.select(s"select userId, name, icon, gitUrl, username from Project")
    projectsItems.map(project _)
  }

  def findUser(profile: PhysalisProfile): Option[User] = findUser(profile.userId)

  private def findUser(userId: String): Option[User] = {
    val profiles = findProfiles(userId)
    val projects = findProjectsByUser(userId)
    val awsItems = userDomain.select(s"""select username,
      fullname,
      email,
      wantsnewsletter
      from User
      where itemName() = '${userId}'""").headOption

    awsItems.map(user(_, projects, profiles(0), profiles))
  }

  private def findProjects(profile: PhysalisProfile): Seq[Project] = findProjectsByUser(profile.userId)

  def findProject(username: String, project: String): Option[Project] = {
    val userOption: Option[User] = findUserByUsername(username)
    if (userOption.isDefined)
      findProjectByUserAndProjectname(userOption.get.id, project)
    else None
  }

  private def findProjectsByUser(userId: String): Seq[Project] = {
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

  private def findProjectByUserAndProjectname(userId: String, projectName: String): Option[Project] = {
    val projectsItems: Seq[Item] = projectsDomain.select(
      s"""select
        name,
        icon,
        gitUrl,
        userId,
        username
        from Project
        where userId = '${userId}' and name = '${projectName}'""")
    projectsItems.map(project _).headOption
  }

  def findProjectById(projectId: String) = {
    val projectsItems = projectsDomain.select(s"""select
        name,
        icon,
        gitUrl,
        userId,
        username
        from Project
			  where itemName() = '${projectId}'""").headOption
    projectsItems.map(project _).headOption
  }

  private def findProfiles(profile: PhysalisProfile): Seq[PhysalisProfile] = findProfiles(profile.userId)

  private def findProfiles(userId: String): Seq[PhysalisProfile] = {
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
          where userId = '${userId}'""")
    profilesItems.map(physalisProfile _)
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

  def saveProfile(profile: PhysalisProfile) = {
    logger.info(s"Save profile ${profile}")

    val profileData = ArrayBuffer(
      "providerId" -> profile.providerId,
      "providerUserId" -> profile.providerUserId,
      "userId" -> profile.userId)

    if (profile.firstName.isDefined) profileData += "firstName" -> profile.firstName.get
    if (profile.lastName.isDefined) profileData += "lastName" -> profile.lastName.get
    if (profile.fullName.isDefined) profileData += "fullName" -> profile.fullName.get
    if (profile.email.isDefined) profileData += "email" -> profile.email.get
    if (profile.avatarUrl.isDefined) profileData += "avatarUrl" -> profile.avatarUrl.get

    profileDomain.put(profile.id, profileData: _*)
    Logger.info(s"saved profile: ${profile.id}")
    profile
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
      usernameOption = username,
      fullnameOption = fullname,
      emailOption = email,
      wantsNewsletter = wantsnewsletter,
      projects = projects,
      main = main,
      identities = identities)
  }
}
