package models

import securesocial.core.BasicProfile

// a simple User class that can have multiple identities
case class Project(id: String, name: String, icon: String, gitUrl: String, version: String)

case class User(id: String,
                username: Some[String],
                fullname: Some[String],
                email: Some[String],
                wantNewsletter: Boolean,
                projects: List[Project],
                main: BasicProfile,
                identities: List[BasicProfile]) {
  val newUser: Boolean = !username.isDefined
}