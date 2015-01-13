package controllers


import securesocial.core._
import service.DemoUser
import play.api.mvc.{ Action, RequestHeader }

class SocialApplication(override implicit val env: RuntimeEnvironment[DemoUser]) extends securesocial.core.SecureSocial[DemoUser] {
//  def index = SecuredAction { implicit request =>
//    Ok(views.html.securesocial.index(request.user.main))
//  }

  // a sample action using an authorization implementation
  def onlyTwitter = SecuredAction(WithProvider("twitter")) { implicit request =>
    Ok("You can see this because you logged in using Twitter")
  }

  def linkResult = SecuredAction { implicit request =>
    Ok(views.html.securesocial.linkResult(request.user))
//   Ok("")
  }

  /**
   * Sample use of SecureSocial.currentUser. Access the /current-user to test it
   */
  def currentUser = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    SecureSocial.currentUser[DemoUser].map { maybeUser =>
      val userId = maybeUser.map(_.main.userId).getOrElse("unknown")
      Ok(s"Your id is $userId")
    }
  }
}

// An Authorization implementation that only authorizes uses that logged in using twitter
case class WithProvider(provider: String) extends Authorization[DemoUser] {
  def isAuthorized(user: DemoUser, request: RequestHeader) = {
    user.main.providerId == provider
  }
}