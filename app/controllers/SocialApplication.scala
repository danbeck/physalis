package controllers

import securesocial.core._
import play.api.mvc.{ Action, RequestHeader }
import models.User

class SocialApplication(override implicit val env: RuntimeEnvironment[User]) extends securesocial.core.SecureSocial[User] {

  // a sample action using an authorization implementation
  def onlyTwitter = SecuredAction(WithProvider("twitter")) { implicit request =>
    Ok("You can see this because you logged in using Twitter")
  }

  def securedURL = SecuredAction {
    Ok("Hallo Karin! Ich bin eine gesicherte Seite!")
  }

  def userAwareURL = UserAwareAction { implicit request =>
    val userName = request.user match {
      case Some(user) => "email:" + user.main.email
      case _          => "guest"
    }
    Ok("Hello %s".format(userName))
  }

  //  def linkResult = SecuredAction { implicit request =>
  //    Ok(views.html.securesocial.linkResult(request.user))
  //    //   Ok("")
  //  }

  /**
   * Sample use of SecureSocial.currentUser. Access the /current-user to test it
   */
  def currentUser = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    SecureSocial.currentUser[User].map { maybeUser =>
      val userId = maybeUser.map(_.main.userId).getOrElse("unknown")
      Ok(s"Your id is $userId")
    }
  }
}

// An Authorization implementation that only authorizes uses that logged in using twitter
case class WithProvider(provider: String) extends Authorization[User] {
  def isAuthorized(user: User, request: RequestHeader) = {
    user.main.providerId == provider
  }
}