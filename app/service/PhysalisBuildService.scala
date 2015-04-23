package service

import models.BuildTask
import play.api.Logger
import scala.sys.process._

/**
 * This is the build service, which builds apps using Apache Cordova.
 * Created by daniel on 23.04.15.
 */
object PhysalisBuildService {

  val logger: Logger = Logger(this.getClass())

  def start = {
    while (true) {
      val buildTasks: Seq[BuildTask] = BuildTask.findNew
      buildTasks.foreach {
        process _
      }
      Thread.sleep(1000)
    }
  }

  def process(buildTask: BuildTask) = {
    Logger.info(s"Process item: ${buildTask}")


    val GitRegex = """https://(.*)/(.*\.git)""".r
    val projectName = buildTask.gitUrl match {
      case GitRegex(_, projectName) => projectName
    }
    Logger.info(s"prrojectname was ${projectName}")

    Logger.info(s"checkout ${buildTask.gitUrl}")

    val command = s"""git clone ${buildTask.gitUrl} --depth=1 ${buildTask.userId}/${buildTask.projectId}"""
    Logger.info(s">> Executing ${command}")
    command.!
  }

  //  def createAndDeleteFakeProject = "sudo docker run danielbeck/cordova-ubuntu cordova create testapp".!
}
