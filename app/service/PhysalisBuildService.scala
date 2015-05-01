package service

import models.BuildTask
import play.api.Logger

import scala.concurrent.Future

/**
 * This is the build service, which builds apps using Apache Cordova.
 * Created by daniel on 23.04.15.
 */
class PhysalisBuildService(platform: String) {

  val logger: Logger = Logger(this.getClass())

  def start() = {
    logger.info(s"Start build service for ${platform}")
    new Thread(() => {
      newBuildTasks.foreach(process _)
      Thread.sleep(1000)
    }).start()
  }

  private def newBuildTasks() = {
    val buildTasks = BuildTask.findNew.filter(task => platform.contains(task.platform))
    logger.info("Found BuildTasks: " + buildTasks.map(t => t.id.toString).mkString(","))
    buildTasks
  }

  def process(buildTask: BuildTask) = {
    logger.info("processing buildtask: " + buildTask.id)
    buildTask.inProgress().save()
    buildTask.gitClone()
    buildTask.startBuilding()
    buildTask.done().save()
  }

  //  def createAndDeleteFakeProject = "sudo docker run danielbeck/cordova-ubuntu cordova create testapp".!
}
