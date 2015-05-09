package models

import java.util.UUID
import service.simpledb.Repository

/**
 * Created by daniel on 23.04.15.
 */

case class Project(id: String = UUID.randomUUID().toString(),
              name: String,
              icon: Option[String],
              gitUrl: String,
              visible: Boolean = true,
              userId: String,
              username: String) {
  def save() = Repository.saveProject(this)

  def lastBuildTask(platform: String): Option[BuildTask] = Repository.findLastBuildTask(this, platform)

}
object Project {
  def findByUsernameAndProjectname(username: String, projectname: String): Option[Project] =
    Repository.findProject(username, projectname)
  def findById(id: String): Option[Project] = Repository.findProjectById(id)
  def findAll(): List[Project] = Repository.findProjects().toList
}
