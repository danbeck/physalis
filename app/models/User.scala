package models

import java.util.Date

case class User(email: String,
                fullname: String,
                username: String,
                projects: List[Project]
//                main: BasicProfile,
//                identities: List[BasicProfile]
) {
  def validated: Boolean = false
}

//case class User(email: String, username: String, fullname: String, projects: List[Project]) {
//}
//
object User {
  def authenticate(email: String, password: String): User = {
    User(email = email, fullname = null, username = null, projects = List())
  }
  def findByEmail(email: String) =
    User(email = email, fullname = null, username = null, projects = List())

}

case class Project(projectName: String, description: String, lastBuild: Date)