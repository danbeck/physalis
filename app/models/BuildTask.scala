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

  private val logger: Logger = Logger(this.getClass)
  private val GitRegex = """https?://(.*)/(.*\.git)""".r

  private val projectName = gitUrl match {
    case GitRegex(_, projectName) => projectName
  }

  private val projectPath = """${userId}/${projectId}"""

  private def persistBuildInProcess(): Unit = {
    logger.info(s"Marked BuildTask ${id} in progress")
  }

  def gitClone() = {
    val command = s"git clone ${gitUrl} --depth=1 ${projectPath}"
    Logger.info(s">> ${command}")
    command.!
  }

  def startBuilding(): Unit = {
    logger.info(s"Build ${projectName} for platform ${platform}")
    def createAndDeleteFakeProject = s"sudo docker run danielbeck/cordova-ubuntu cordova build ${projectPath}".!
  }

}

object BuildTask {
  def findNew = Repository.findNewBuildTasks()
  def createNew(project: Project, user: User) = {
//    BuildTask()
  }

}