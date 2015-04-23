package models

import service.simpledb.Repository

/**
 * Created by daniel on 23.04.15.
 */
case class BuildTask(id: String, projectId: String, userId: String, gitUrl: String, platform: String)

object BuildTask {
  def findNew = Repository.findNewBuildTasks()
}