package models

import securesocial.core.BasicProfile
import securesocial.core.GenericProfile
import securesocial.core.AuthenticationMethod
import securesocial.core.OAuth1Info
import securesocial.core.OAuth2Info
import securesocial.core.PasswordInfo
import service.simpledb.Repository
import java.util.UUID

case class PhysalisProfile(id: String = UUID.randomUUID().toString(),
                           providerId: String,
                           providerUserId: String,
                           firstName: Option[String],
                           lastName: Option[String],
                           fullName: Option[String],
                           email: Option[String],
                           avatarUrl: Option[String],
                           authMethod: AuthenticationMethod = null,
                           userId: String = UUID.randomUUID().toString(),
                           oAuth1Info: Option[OAuth1Info] = None,
                           oAuth2Info: Option[OAuth2Info] = None,
                           passwordInfo: Option[PasswordInfo] = None) extends GenericProfile {
  def toBasicProfile() =
    BasicProfile(providerId = providerId,
      userId = providerUserId,
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      email = email,
      avatarUrl = avatarUrl,
      authMethod = authMethod)

  def save() = Repository.saveProfile(this)

}

object PhysalisProfile {
  def create(p: BasicProfile) = PhysalisProfile(providerId = p.providerId,
    providerUserId = p.userId,
    firstName = p.firstName,
    lastName = p.lastName,
    fullName = p.fullName,
    email = p.email,
    avatarUrl = p.avatarUrl)

  def create(p: BasicProfile, id: String, userId: String) = PhysalisProfile(id = id,
    providerId = p.providerId,
    providerUserId = p.userId,
    firstName = p.firstName,
    lastName = p.lastName,
    fullName = p.fullName,
    email = p.email,
    avatarUrl = p.avatarUrl,
    userId = userId)
}

case class Project(id: String = UUID.randomUUID().toString(),
                   name: String,
                   icon: Option[String],
                   gitUrl: String,
                   userId: String,
                   username: String) {
  def save() = {
    Repository.saveProject(this)
    this
  }
}

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
