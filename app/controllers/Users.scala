package controllers

import org.joda.time.DateTime

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.RequestHeader
//    import models.DemoUser

//class Users{
//
//  
//}

object Users extends Controller {
  def index(username: String) = Action { implicit requestHeader: RequestHeader =>

//    val projects = List(
//      Project("Green Mahjong", "Green Mahjong is a HTML5 Game", DateTime.now.toDate),
//      Project("RSS Reader", "Green Mahjong is a HTML5 Game", DateTime.now.toDate))
//
//    val user = User(email = "email@gmail.com", fullname = "Daniel Beck", username = username, projects = projects)
//    Ok(views.html.user(user))
    Ok("Users.index")
  }
}