package service

import awscala.simpledb.SimpleDBClient
import awscala.simpledb.Domain
import models.User
import scala.concurrent.Future
import securesocial.core.providers.MailToken
import securesocial.core.BasicProfile
import securesocial.core.PasswordInfo
import securesocial.core.services.SaveMode
import play.api.Logger
import java.util.UUID
import models.Project

class SimpleDBUserService extends UpdatableUserService {

  implicit val simpleDB = new SimpleDBClient()
  val usersDomain: Domain = simpleDB.createDomain("user")
  val projectsDomain: Domain = simpleDB.createDomain("project")
  val basicProfileDomain: Domain = simpleDB.createDomain("basicProfile")

  def save(project: Project) = {
    Logger.info(s"SimpleDB: Adding project '${project.id}' '${project.name}' '${project.gitUrl}'")
    projectsDomain.put(project.id,
      "gitUrl" -> project.gitUrl,
      "user" -> project.user.username.get,
      "project" -> project.name,
      "state" -> "NEW",
      "s3Url" -> "")

  }
  //  def saveUser(user: User) = {
  //    Logger.info("creating simpledb")
  //    implicit val simpleDB = new SimpleDBClient()
  //    //    val region: Region = new Region()
  //    Logger.info("creating domain")
  //    val domain: Domain = simpleDB.createDomain("users")
  //    domain.put("daniel", "name" -> "Daniel Beck", "email" -> "d.danielbeck@googlemail.com")
  //    domain.put("karin", "name" -> "Karin Beck", "email" -> "kbeck@googlemail.com")
  //
  //    val items: Seq[Item] = domain.select(s"select * from users where name = 'Daniel Beck'")
  //
  //    Logger.info("items: " + items)
  //    Logger.error("items: " + items)
  //    simpleDB.domains.foreach(_.destroy())
  //
  //  }
  // Members declared in service.UpdatableUserService
  def find(username: String): Future[Option[User]] = {
    Logger.info(s"find '$username")
    null
  }
  def projects: List[models.Project] = { List() }
  def update(user: models.User): models.User = { null }

  def deleteExpiredTokens(): Unit = {}
  def deleteToken(uuid: String): Future[Option[MailToken]] = { null }
  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    Logger.info(s"find '$providerId' '$userId")
    Future.successful(None)
  }

  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Logger.info(s"findByEmail '$providerId' '$email")
    null
  }
  def findToken(token: String): Future[Option[MailToken]] = { null }
  def link(current: User, to: BasicProfile): Future[User] = { null }
  def passwordInfoFor(user: User): Future[Option[PasswordInfo]] = { null }
  def save(profile: BasicProfile, mode: SaveMode): Future[models.User] = {
    val newUser: User = mode match {
      case SaveMode.SignUp         => User(id = UUID.randomUUID().toString, main = profile, identities = List(profile))
      case SaveMode.LoggedIn       => null
      case SaveMode.PasswordChange => null
    }
    saveNewUser()

    def saveNewUser() = {
      val profileid = UUID.randomUUID().toString()
      //      
      //      providerId = "github",
      //        userId = "karin",
      //        firstName = Some("Karin"),
      //        lastName = Some("Beck"),
      //        fullName = Some("Karin Beck"),
      //        email = Some("karin@freen.et"),
      //        avatarUrl = Some("www"),
      //        authMethod = null,
      //        oAuth1Info = None,
      //        oAuth2Info = None,
      //        passwordInfo = None)
      //        
      //        

      basicProfileDomain.put(profileid, "providerId" -> profile.providerId)

    }
    //    usersDomain.put(newUser.id, "username" -> newUser.username, "fullname" -> newUser.fullname, "email" -> newUser.email)
    Future.successful(newUser)
  }
  def saveToken(token: MailToken): Future[MailToken] = { null }
  def updatePasswordInfo(user: models.User, info: PasswordInfo): Future[Option[BasicProfile]] = { null }
}