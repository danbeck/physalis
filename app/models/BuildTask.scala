package models

import play.api.Logger
import service.simpledb.Repository
import scala.sys.process._
import java.util.UUID

/**
 * Created by daniel on 23.04.15.
 */
case class BuildTask(
  id: String = UUID.randomUUID().toString(),
  project: Project,
  user: User,
  state: String = "NEW",
  s3Url: Option[String] = None,
  platform: String) {

  private val projectPath = s"${user.id}/${project.id}/$platform"
  private val logger: Logger = Logger(this.getClass)

  private val projectName = project.gitUrl match {
    case BuildTask.validGitUrlRegex(projectName) => projectName
  }

  def save(): BuildTask = {
    logger.info(s"Save BuildTask ${this.id}")
    Repository.saveBuildTask(this)
  }

  def inProgress(): BuildTask = this.copy(state = "IN_PROGRESS")

  def done(s3Url: Option[String] = None): BuildTask = this.copy(state = "DONE", s3Url = s3Url)

  def error(): BuildTask = this.copy(state = "ERROR")

  def gitClone() = {
    val command = s"rm -rf ${projectPath}"
    Logger.info(s">> ${command}")
    command.!
    val cloneCommand = s"git clone ${project.gitUrl} --depth=1 ${projectPath}"
    Logger.info(s">> ${cloneCommand}")
    cloneCommand.!
  }

  def startBuilding(): Unit = {
    logger.info(s"Build ${projectName} (gitURL was: ${project.gitUrl}) for platform $platform")
    //    def createAndDeleteFakeProject = s"sudo docker run danielbeck/cordova-ubuntu cordova build ${projectPath}".!
  }

}

object BuildTask {
  def findNew = Repository.findNewBuildTasks()
  val validGitUrlRegex = """https?://.*/(.*)\.git""".r
  val validGitUrlPattern = validGitUrlRegex.pattern
  def createNew(project: Project, user: User) = {
    //    BuildTask()
  }

}