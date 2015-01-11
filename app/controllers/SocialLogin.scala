package controllers

import play.api.mvc.Controller
import org.brickred.socialauth.SocialAuthConfig
import play.api.mvc.Action
import org.brickred.socialauth.SocialAuthManager
import org.brickred.socialauth.util.SocialAuthUtil
import org.brickred.socialauth.util.AccessGrant
import org.brickred.socialauth.AuthProvider
import org.brickred.socialauth.Profile
import play.twirl.api.Html
import collection.JavaConversions._

object SocialLogin extends Controller {
  val manager: SocialAuthManager = new SocialAuthManager()
  val config = SocialAuthConfig.getDefault()
  config.load()
  manager.setSocialAuthConfig(config)

  def login = Action {
    val successUrl: String = "http://physalis.io/login/github/response"
    val authentificationUrl: String = manager.getAuthenticationUrl("github", successUrl);
    Ok(Html("""<html><body><h1>To continue, Login</h1>
      <a href="""" + authentificationUrl + """,repo">Login to github</a></body></html>"""))
  }

  def res = Action { implicit request =>
    val paramMap: Map[String, String] = request.queryString.map { case (k, v) => k -> v.mkString }
    val provider: AuthProvider = manager.connect(mapAsJavaMap(paramMap));

    val profile: Profile = provider.getUserProfile()
    Ok(profile+ "")
  }
}