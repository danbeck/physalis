package controllers

import play.api.mvc.Controller
import play.api.mvc.RequestHeader
import securesocial.core.SecureSocial
import play.api.mvc.Action

object Users extends Controller with SecureSocial {

  def index(username: String) = Action { implicit requestHeader: RequestHeader =>
    Ok(views.html.user(null))
  }

}