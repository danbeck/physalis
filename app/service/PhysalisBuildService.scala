package service

import models.BuildTask
import play.api.Logger
import scala.concurrent.Future
import java.util.concurrent.Executors

/**
 * This is the build service, which builds apps using Apache Cordova.
 * Created by daniel on 23.04.15.
 */
object PhysalisBuildService {

  val logger: Logger = Logger(this.getClass())
  var thread: Option[Thread] = None
  var running = true;
  val threadPool = Executors.newFixedThreadPool(10);

  def start(platforms: Option[List[String]]) = {
    logger.info(s"----------START BUILDSERVICE----------!")
    logger.info(s"Supported platforms: ${platforms}")
    if (platforms.isDefined) {
      running = true;
      thread = Some(new Thread(() => {
        while (running) {
          processNewBuildTasks(platforms.get)
        }
      }))
      thread.get.start()
    }
  }

  def processNewBuildTasks(platforms: List[String]) {
    try {
      newBuildTasks(platforms).foreach(processAndHandleExceptions _)
      Thread.sleep(3500)
    } catch {
      case e: Exception => logger.error("there was an exception in the main build loop. Continuing.", e)
    }
  }

  def stop() = {
    logger.info("----------STOPPING BUILDSERVICE--------!")
    threadPool.shutdown()
    if (thread.isDefined) {
      running = false;
      thread.get.join()
    }
  }

  private def newBuildTasks(platforms: Seq[String]) = {
    //    def info(tasks: Seq[BuildTask]) = tasks.map(t => s"${t.platform}->${t.id},${t.project.gitUrl}")

    val tasks = BuildTask.findNew(platforms)
    if (!tasks.isEmpty) logger.info(s"$platforms - found BuildTasks: " + tasks.map(t => t.id.toString).mkString(","))
    tasks
  }

  def processAndHandleExceptions(buildTask: BuildTask) = {
    try {
      threadPool.submit(new Runnable() {
        def run() = buildTask.execute()
      })
    } catch {
      case t: Throwable => logger.error(s"Got error in Build-Service: ${t.getMessage}", t);
    }
  }
}
