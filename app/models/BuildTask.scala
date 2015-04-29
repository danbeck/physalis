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
  projectId: String,
  userId: String,
  gitUrl: String,
  platform: String) {

  private val projectPath = s"${userId}/${projectId}"
  private val logger: Logger = Logger(this.getClass)

  private val projectName = gitUrl match {
    case BuildTask.validGitUrlRegex(projectName) => projectName
    case _                                       => "didnt match"
  }

  private def persistBuildInProcess(): Unit = {
    logger.info(s"Marked BuildTask ${id} in progress")
  }

  def gitClone() = {
    val command = s"git clone ${gitUrl} --depth=1 ${projectPath}"
    Logger.info(s">> ${command}")
    command.!
  }

  def startBuilding(): Unit = {
    logger.info(s"Build ${projectName} (gitURL was: $gitUrl) for platform $platform")
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