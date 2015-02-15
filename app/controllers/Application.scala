package controllers

import play.Logger
import play.api.data.Form
import play.api.mvc.{Session, Result, Controller, Action, RequestHeader}
import play.api.data.validation.Constraints
import play.api.i18n.Messages
import play.api.mvc.Flash
import securesocial.core.java.UserAwareAction
import securesocial.core.RuntimeEnvironment
import models.User
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.services.simpledb.AmazonSimpleDBClient
import com.amazonaws.services.simpledb.model.CreateDomainRequest
import com.amazonaws.services.simpledb.model.ReplaceableItem
import service.SimpleDBRepository

class Application(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  //  def userAwareURL = UserAwareAction { implicit request =>
  //    val userName = request.user match {
  //      case Some(user) => "email:" + user.main.email
  //      case _          => "guest"
  //    }
  //    Ok("Hello %s".format(userName))
  //  }

  def testSimpleDB = Action { implicit request =>
    SimpleDBRepository.saveUser(null)
    Ok("done")
  }

  def loginRedirectURI = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) if u.email != None => Redirect(accounts.routes.Index.user(u.username.get))
      case Some(u) => Ok("")
      //        env.userService.find(u.main, userId)
      //        Ok("")
      case None => BadRequest(views.html.index(null))
    }
  }

  def signupRedirectURI = UserAwareAction { implicit request =>

    Ok("")

  }

  def index = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) => Ok(views.html.index(u))
      case None => Ok(views.html.index(null))
    }
  }

  def signup = UserAwareAction { implicit request =>
    Ok(views.html.signup(null))
  }

//  val productForm = Form(mapping(
//    "ean" -> longNumber,
//    "name" -> nonEmptyText,
//    "description" -> text,
//    "pieces" -> number,
//    "active" -> boolean)(Product.apply)(Product.unapply))


  def showEnterUserDataForm = UserAwareAction { implicit request =>
    request.user match {
      // case Some(u) => Ok(views.html.signupEnterUserData(u,))
      case Some (u) => Ok("")
    }
  }

  def postUserData = UserAwareAction {
    implicit request =>
      System.out.println("posted data!")
      Ok("Data posted")
  }

  def logout = Action {
    Redirect(routes.CustomLoginController.logout).flashing("success" -> Messages("youve.been.logged.out"))
  }

  def login = Action { implicit request =>
    Ok(views.html.login(null))
  }
}