package models

import securesocial.core.BasicProfile
import service.SimpleDBRepository
import securesocial.core.BasicProfile
import securesocial.core.GenericProfile
import securesocial.core.AuthenticationMethod
import securesocial.core.OAuth1Info
import securesocial.core.OAuth2Info
import securesocial.core.PasswordInfo
import securesocial.core.BasicProfile
import service.SimpleDBService
import java.util.UUID

case class PhysalisProfile(id: String = UUID.randomUUID().toString(),
                           providerId: String,
                           profileId: String,
                           firstName: Option[String],
                           lastName: Option[String],
                           fullName: Option[String],
                           email: Option[String],
                           avatarUrl: Option[String],
                           authMethod: AuthenticationMethod = null,
                           userId: String // 
                           // oAuth1Info: Option[OAuth1Info] = None,
                           // oAuth2Info: Option[OAuth2Info] = None,
                           // passwordInfo: Option[PasswordInfo] = None
                           ) extends GenericProfile {
  def toBasicProfile() =
    BasicProfile(providerId = providerId,
      userId = profileId,
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      email = email,
      avatarUrl = avatarUrl,
      authMethod = authMethod)

  def save() = SimpleDBService.saveProfile(this)

}

case class Project(id: String, name: String, icon: String, gitUrl: String) {
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
