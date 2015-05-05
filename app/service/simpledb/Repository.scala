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
import java.time.Instant

object Repository {
  val logger: Logger = Logger(this.getClass())

  implicit val simpleDB = new SimpleDBClient()
  private val userDomain: Domain = simpleDB.createDomain("User")
  private val projectsDomain: Domain = simpleDB.createDomain("Project")
  private val profileDomain: Domain = simpleDB.createDomain("Profile")
  private val buildTasks: Domain = simpleDB.createDomain("BuildTask")

  def findBuildTasks(id: String): Option[BuildTask] = {
    val items = buildTasks.select(s"""select projectId,
        userId,
        s3Url,
        state,
        platform,
        created,
        updated
      from BuildTask 
      where itemname() = '$id'""")

    items.map(item => {
      val projectId = attrValue(item, "projectId")
      val userId = attrValue(item, "userId")
      val s3Url = attrOption(item, "s3Url")
      val project = findProjectById(projectId, true).get
      val user = findUser(userId).get
      val platform = attrValue(item, "platform")
      val state = attrValue(item, "state")

      BuildTask(id = item.name,
        project = project,
        user = user,
        state = state,
        s3Url = s3Url,
        platform = platform)
    }).headOption
  }

  def findNewBuildTasks(platform: String): Seq[BuildTask] = {
    val items = buildTasks.select(s"""select projectId,
      userId,
      s3Url
      from BuildTask 
      where state = 'NEW' and platform = '$platform'""")

    items.map(item => {
      val projectId = attrValue(item, "projectId")
      val userId = attrValue(item, "userId")
      val s3Url = attrOption(item, "s3Url")
      val project = findProjectById(projectId).get
      val user = findUser(userId).get

      BuildTask(id = item.name,
        project = project,
        user = user,
        state = "NEW",
        s3Url = s3Url,
        platform = platform)
    })
  }

  def findLastBuildTask(project: Project, platform: String): Option[BuildTask] = {
    val items = buildTasks.select(s"""select userId,
      s3Url,
      state,
      created,
      updated
      from BuildTask 
      where platform = '$platform'
      order by created desc""")

    // TODO: only take the first BuildTask.
    items.headOption.map {
      item =>
        val userId = attrValue(item, "userId")
        val created = Instant.parse(attrValue(item, "created"))
        val updated = Instant.parse(attrValue(item, "updated"))
        val state = attrValue(item, "state")
        val s3Url = attrOption(item, "s3Url")
        val user = findUser(userId).get

        BuildTask(id = item.name,
          project = project,
          user = user,
          state = state,
          s3Url = s3Url,
          platform = platform,
          created = created,
          updated = updated)
    }
  }

  def saveBuildTask(task: BuildTask) = {
    val data = ArrayBuffer("projectId" -> task.project.id,
      "userId" -> task.user.id,
      "platform" -> task.platform,
      "state" -> task.state,
      "created" -> task.created.toString(),
      "updated" -> task.updated.toString())
    if (task.s3Url.isDefined) data += "s3Url" -> task.s3Url.get
    buildTasks.replaceIfExists(task.id, data: _*)
    task
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
      wantsnewsletter,
      accountPlan
      from User
      where username = '${username}'""").headOption
    awsUser.map(_.name) match {
      case Some(itemId) => findUser(itemId)
      case None         => None
    }
  }

  def findUser(profile: PhysalisProfile): Option[User] = findUser(profile.userId)

  private def findUser(userId: String): Option[User] = {
    val profiles = findProfiles(userId)
    val projects = findProjectsByUser(userId)
    val awsItems = userDomain.select(s"""select username,
      fullname,
      email,
      wantsnewsletter,
      accountPlan
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

  def findProjects(): Seq[Project] = {
    val projectsItems = projectsDomain
      .select(s"""select 
                  userId, 
                  name, 
                  icon, 
                  gitUrl, 
                  username, 
                  visible
                from Project
                where visible = 'true'""")
    projectsItems.map(item => project(item, false))
  }

  def saveProject(project: Project) = {
    logger.info(s"Save project '${project}")

    val projectData = ArrayBuffer(
      "name" -> project.name,
      "gitUrl" -> project.gitUrl,
      "userId" -> project.userId,
      "visible" -> project.visible.toString(),
      "username" -> project.username)

    if (project.icon.isDefined) projectData += ("icon" -> project.icon.get)
    projectsDomain.put(project.id, projectData: _*)
  }

  private def findProjectsByUser(userId: String): Seq[Project] = {
    val projectsItems = projectsDomain.select(
      s"""select
          name,
          icon,
          gitUrl,
          visible,
          userId,
          username
        from Project
        where userId = '${userId}'""")
    val projects = projectsItems.map(item => project(item, false))
    projects
  }

  private def findProjectByUserAndProjectname(userId: String, projectName: String): Option[Project] = {
    val projectsItems: Seq[Item] = projectsDomain.select(
      s"""select
          name,
          icon,
          gitUrl,
          visible,
          userId,
          username
        from Project
        where userId = '${userId}' and name = '${projectName}'""")
    projectsItems.map(item => project(item, false)).headOption
  }

  def findProjectById(projectId: String, shallow: Boolean = false) = {
    val projectsItems = projectsDomain.select(s"""select
          name,
          icon,
          gitUrl,
          visible,
          userId,
          username
        from Project
			  where itemName() = '${projectId}'""").headOption
    projectsItems.map(item => project(item, shallow)).headOption
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

  private def project(item: Item, shallow: Boolean) = {
    val userId = attrValue(item, "userId")
    val name = attrValue(item, "name")
    val icon = attrOption(item, "icon")
    val gitUrl = attrValue(item, "gitUrl")
    val username = attrValue(item, "username")
    val visible = attrValue(item, "visible") == "true"
    Project(id = item.name,
      name = name,
      icon = icon,
      gitUrl = gitUrl,
      visible = visible,
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
    val accountPlan = attrValue(item, "accountPlan")

    User(id = item.name,
      usernameOption = username,
      fullnameOption = fullname,
      emailOption = email,
      accountPlan = accountPlan,
      wantsNewsletter = wantsnewsletter,
      projects = projects,
      main = main,
      identities = identities)
  }
}
