package models

import java.util.UUID

import securesocial.core._
import service.simpledb.Repository

/**
 * Created by daniel on 23.04.15.
 */
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

  def basicProfile() = BasicProfile(providerId = providerId,
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

  def find(profile: BasicProfile): Option[PhysalisProfile] = find(profile.providerId, profile.userId)

  def find(providerId: String, userId: String): Option[PhysalisProfile] = Repository.findPhysalisProfile(providerId, userId)
}
