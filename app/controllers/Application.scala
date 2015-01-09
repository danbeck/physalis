package controllers

import models.User
import play.Logger
import play.api.mvc.{ Session, Result, Controller, Action, RequestHeader }
import play.api.data.validation.Constraints
//import play.api.dadata.validation.Constraints.Required
import play.api.i18n.Messages
import play.api.mvc.Flash

object Application extends Controller {

  def index = Action { implicit requestHeader: RequestHeader =>
    val session: Session = requestHeader.session;
    val email = session.get("email")

    val user: Option[User] = email match {
      case Some(e) => Some(User.findByEmail(e))
      case None    => None
    }

    user match {
      case None                     => Ok(views.html.index(null))
      case Some(u) if (u.validated) => Ok(views.html.index(u))
      case Some(u) => {
        Logger.debug("clearing invalid session creditentials")
        //        session.clear()
        Ok(views.html.index(u))
      }
    }
  }

  def logout = Action {
    Redirect(routes.Application.index).flashing("success" -> Messages("youve.been.logged.out"))

  }

  def login = Action { implicit Request =>
    Ok(views.html.index(null))
  }

  case class Login(val email: String,
                   val password: String) {
    /**
     * Validates the authentication. Returns null if validation was ok
     */
    def validate: String = {
      val user: Option[User] = Option(User.authenticate(email, password))
      //    TODO: catch exception!
      user match {
        case None                      => Messages("invalid.user.or.password")
        case Some(u) if (!u.validated) => Messages("account.not.validated.check.mail")
        case _                         => null
      }
    }

  }
}