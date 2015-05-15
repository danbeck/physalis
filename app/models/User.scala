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
                usernameOption: Option[String] = None,
                fullnameOption: Option[String] = None,
                emailOption: Option[String] = None,
                wantsNewsletter: Boolean = false,
                accountPlan: String = "onlyPublic",
                projects: List[Project] = List(),
                main: PhysalisProfile,
                identities: List[PhysalisProfile]) {

  val newUser: Boolean = !usernameOption.isDefined

  def save(): User = Repository.saveUser(this)
}

object User {
  def findByUsername(username: String): Option[User] = Repository.findUserByUsername(username)
  def findByEmail(email: String): Option[User] = Repository.findUserByEmail(email)
  def find(profile: PhysalisProfile): Option[User] = Repository.findUser(profile)
}
