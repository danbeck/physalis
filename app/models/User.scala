package models

import java.util.Date

case class User(email: String, username: String, fullname: String, projects: List[Project]) {
  def validated: Boolean = false
}

object User {
  def authenticate(email: String, password: String): User = {
    User(email, null, null, List())
  }

  def findByEmail(email: String) = User(email, null, null, List())
}

case class Project(projectName: String, description: String, lastBuild: Date)