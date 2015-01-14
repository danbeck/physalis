package controllers

import securesocial.controllers.ViewTemplates
import play.api.data.Form
import play.api.mvc.RequestHeader
import play.api.i18n.Lang
import play.twirl.api.Html
import securesocial.core.RuntimeEnvironment

class SecureSocialViewTemplate(env: RuntimeEnvironment[_])  extends ViewTemplates {
  implicit val implicitEnv = env

  def getLoginPage(form: Form[(String, String)], msg: Option[String] = None) // 
  (implicit request: RequestHeader): Html = {

    securesocial.views.html.login(form, msg)
  }

}