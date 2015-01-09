package controllers

import play.api.mvc.Controller
import play.api.mvc.RequestHeader
import securesocial.core.SecureSocial
import play.api.mvc.Action
import models.User
import models.Project
import models.Project
import org.joda.time.DateTime

object Users extends Controller with SecureSocial {

  def index(username: String) = Action { implicit requestHeader: RequestHeader =>

    val projects = List(
      Project("Green Mahjong", "Green Mahjong is a HTML5 Game", DateTime.now.toDate),
      Project("RSS Reader", "Green Mahjong is a HTML5 Game", DateTime.now.toDate))

    val user = User(email = "email@gmail.com", fullname = "Daniel Beck", username = username, projects = projects)
    Ok(views.html.user(user))
  }

}