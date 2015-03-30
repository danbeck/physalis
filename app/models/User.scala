package models

import securesocial.core.BasicProfile
import service.SimpleDBRepository

case class Project(id: String, name: String, icon: String, gitUrl: String, user: User) {
  def save() = {
    SimpleDBRepository.save(this)
    this
  }
}

case class User(id: String,
                username: Option[String] = None,
                fullname: Option[String] = None,
                email: Option[String] = None,
                wantNewsletter: Boolean = false,
                projects: List[Project] = List(),
                main: BasicProfile,
                identities: List[BasicProfile]) {
  val newUser: Boolean = !username.isDefined
}