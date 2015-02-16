package models

import securesocial.core.BasicProfile

// a simple User class that can have multiple identities
case class Project(id: String, name: String, icon: String, gitUrl: String, version: String)

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