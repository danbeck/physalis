package controllers

import play.api.mvc.Controller
import play.api.mvc.RequestHeader
import securesocial.core.SecureSocial

object Users extends Controller with SecureSocial {

  def index(username: String) = SecuredAction { implicit requestHeader: RequestHeader =>
    JavaContext.withContext {
      Ok("Hello " + username )
    }
  }

}