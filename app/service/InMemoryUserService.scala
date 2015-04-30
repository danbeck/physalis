/**
 * Original work: SecureSocial (https://github.com/jaliss/securesocial)
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Derivative work: Physalis (https://github.com/danbeck/physalis)
 * Modifications Copyright 2014-2015 Daniel Beck (d.danielbeck at googlemail dot com)- twitter:  @ddanieltwitt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service

import java.util.UUID
import play.api.Logger
import securesocial.core._
import securesocial.core.providers.{ UsernamePasswordProvider, MailToken }
import scala.concurrent.Future
import securesocial.core.services.{ UserService, SaveMode }
import models.{ User, Project }
import models.PhysalisProfile

/**
 * A Sample In Memory user service in Scala
 *
 * IMPORTANT: This is just a sample and not suitable for a production environment since
 * it stores everything in memory.
 */
class InMemoryUserService extends UpdatableUserService {
  val logger = Logger("application.controllers.InMemoryUserService")

  //
  var users = Map[(String, String), User]()
  //private var identities = Map[String, BasicProfile]()
  private var tokens = Map[String, MailToken]()

  def projects: List[Project] = {

    logger.info("get Projects")
    var result = List[Project]()
    for (
      u <- users
    ) {
      logger.info("user: " + u)
      logger.info("user projects" + u._2.projects)
    }

    val allProjs = for (
      u <- users;
      proj <- u._2.projects
    ) {
      logger.info("project: " + proj)
      result = proj :: result;
    }
    result
    //    allProjs.toList
  }

  def update(user: User): User = {
    users = users + ((user.main.providerId, user.main.userId) -> user)
    user
  }

  def findUserByUsername(username: String): Future[Option[User]] = {
    Future.successful({
      val optionalUserMappingTupple = users.find(_._2.usernameOption == Some(username))
      optionalUserMappingTupple match {
        case Some(tupple) => Some(tupple._2)
        case _            => None
      }
    })
  }

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    if (logger.isDebugEnabled) {
      logger.debug("users = %s".format(users))
    }
    val result = for (
      user <- users.values;
      basicProfile <- user.identities.find(su => su.providerId == providerId && su.userId == userId)
    ) yield {
      basicProfile
    }
    Future.successful(result.headOption.map {
      _.basicProfile
    })
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    if (logger.isDebugEnabled) {
      logger.debug("users = %s".format(users))
    }
    val someEmail = Some(email)
    val result = for (
      user <- users.values;
      basicProfile <- user.identities.find(su => su.providerId == providerId && su.email == someEmail)
    ) yield {
      basicProfile
    }
    Future.successful(result.headOption.map(_.basicProfile))
  }

  private def findProfile(p: PhysalisProfile) = {
    users.find {
      case (key, value) if value.identities.exists(su => su.providerId == p.providerId && su.userId == p.userId) => true
      case _ => false
    }
  }

  private def updateProfile(user: PhysalisProfile, entry: ((String, String), User)): Future[User] = {
    val identities = entry._2.identities
    val updatedList = identities.patch(identities.indexWhere(i => i.providerId == user.providerId && i.userId == user.userId), Seq(user), 1)
    val updatedUser = entry._2.copy(identities = updatedList)
    users = users + (entry._1 -> updatedUser)
    Future.successful(updatedUser)
  }

  def save(basicProfile: BasicProfile, mode: SaveMode): Future[User] = {
    val user = PhysalisProfile.create(basicProfile)
    mode match {
      case SaveMode.SignUp =>
        val newUser = User(id = UUID.randomUUID().toString,
          main = user,
          identities = List(user))

        users = users + ((user.providerId, user.userId) -> newUser)

        Future.successful(newUser)
      case SaveMode.LoggedIn =>
        // first see if there is a user with this BasicProfile already.
        findProfile(user) match {
          case Some(existingUser) =>
            updateProfile(user, existingUser)

          case None =>
            val newUser = User(id = UUID.randomUUID().toString,
              main = user,
              identities = List(user))
            users = users + ((user.providerId, user.userId) -> newUser)
            Future.successful(newUser)
        }

      case SaveMode.PasswordChange =>
        findProfile(user).map { entry => updateProfile(user, entry) }.getOrElse(
          // this should not happen as the profile will be there
          throw new Exception("missing profile)"))
    }
  }

  def link(current: User, to: BasicProfile): Future[User] = {
    if (current.identities.exists(i => i.providerId == to.providerId && i.userId == to.userId)) {
      Future.successful(current)
    } else {
      val added = PhysalisProfile.create(to) :: current.identities
      val updatedUser = current.copy(identities = added)
      users = users + ((current.main.providerId, current.main.userId) -> updatedUser)
      Future.successful(updatedUser)
    }
  }

  def saveToken(token: MailToken): Future[MailToken] = {
    Future.successful {
      tokens += (token.uuid -> token)
      token
    }
  }

  def findToken(token: String): Future[Option[MailToken]] = {
    Future.successful {
      tokens.get(token)
    }
  }

  def deleteToken(uuid: String): Future[Option[MailToken]] = {
    Future.successful {
      tokens.get(uuid) match {
        case Some(token) =>
          tokens -= uuid
          Some(token)
        case None => None
      }
    }
  }

  //  def deleteTokens(): Future {
  //    tokens = Map()
  //  }

  def deleteExpiredTokens() {
    tokens = tokens.filter(!_._2.isExpired)
  }

  //not needed
  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = {
    null
  }

  // not needed
  def updatePasswordInfo(user: models.User, info: PasswordInfo): Future[Option[BasicProfile]] = {
    null
  }

}


