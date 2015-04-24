package service

import models.BuildTask
import play.api.Logger

import scala.concurrent.Future

/**
 * This is the build service, which builds apps using Apache Cordova.
 * Created by daniel on 23.04.15.
 */
class PhysalisBuildService(buildPlatforms: Seq[String]) {

  val logger: Logger = Logger(this.getClass())

  def start = {
    new Thread(new Runnable {
      def run() {
        logger.info("Start the physalis build service")
        while (true) {
          newBuildTasks.foreach(process _)
          Thread.sleep(1000)
        }
      }
    }).start()
  }

  private def newBuildTasks() = {
    BuildTask.findNew.filter(task => buildPlatforms.contains(task.platform))
  }


  def process(buildTask: BuildTask) = {
    buildTask.gitClone()
    buildTask.startBuilding()
  }

  //  def createAndDeleteFakeProject = "sudo docker run danielbeck/cordova-ubuntu cordova create testapp".!
}
