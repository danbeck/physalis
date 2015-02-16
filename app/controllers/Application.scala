package controllers

import play.Logger
import play.api.data.Form
import play.api.mvc.{ Session, Result, Controller, Action, RequestHeader }
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

  def testSimpleDB = Action { implicit request =>
    SimpleDBRepository.saveUser(null)
    Ok("done")
  }

  def index = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) => Ok(views.html.index(u))
      case None    => Ok(views.html.index(null))
    }
  }

  def signupRedirectURI = UserAwareAction { implicit request =>
    request.user match {
      case Some(u) if u.newUser  => Redirect(accounts.routes.Signup.showEnterUserDataForm)
      case Some(u) if !u.newUser => Redirect(accounts.routes.Index.user(u.username.get))
      case None                  => Redirect(routes.Application.index)
    }
  }

}