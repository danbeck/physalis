package models

import securesocial.core.BasicProfile
import securesocial.core.GenericProfile
import securesocial.core.AuthenticationMethod
import securesocial.core.OAuth1Info
import securesocial.core.OAuth2Info
import securesocial.core.PasswordInfo
import service.simpledb.Repository
import java.util.UUID

case class User(id: String,
                username: Option[String] = None,
                fullname: Option[String] = None,
                email: Option[String] = None,
                wantNewsletter: Boolean = false,
                projects: List[Project] = List(),
                main: PhysalisProfile,
                identities: List[PhysalisProfile]) {

  val newUser: Boolean = !username.isDefined

  def save() = {
    Repository.saveUser(this)
    this
  }
}

object User {
  def findByUsername(userId: String): Option[User] = Repository.findUserByUsername(userId)
}
