/**
 * Original work: SecureSocial (https://github.com/jaliss/securesocial)
 * Copyright 2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Derivative work: Physalis (https://github.com/danbeck/physalis)
 * Modifications Copyright 2014-2015 Daniel Beck (d.danielbeck at googlemail dot com)- twitter:  @ddanieltwitt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.lang.reflect.Constructor

import controllers.CustomRoutesService
import models.{PhysalisProfile, User}
import play.Play
import play.api.Logger
import play.api.Application
import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import securesocial.views.html.Registration
import play.twirl.api.Html
import securesocial.controllers.{ChangeInfo, RegistrationInfo, ViewTemplates}
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers.GitHubProvider
import securesocial.core.services.UserService
import service.{PhysalisBuildService, InMemoryUserService, SecureSocialEventListener}
import service.simpledb.{UserService => SimpleDBUserService}

import scala.collection.immutable.ListMap

object Global extends play.api.GlobalSettings {

  val logger: Logger = Logger.apply(this.getClass)

  override def onStart(app: Application) = {
    val platformsToBuildFor: Option[String] = app.configuration.getString("PLATFORMS_TO_BUILD_FOR")

    val platforms: Option[Array[String]] = platformsToBuildFor.map(string => string.split(","))
    if (platforms.isDefined) {
      val buildService = new PhysalisBuildService(platforms.get)
      buildService.start
    }
  }

  override def onStop(app: Application) = {
    logger.info("Physalis shutdown...")
  }

  /**
   * An implementation that checks if the controller expects a RuntimeEnvironment and
   * passes the instance to it if required.
   *
   * This can be replaced by any DI framework to inject it differently.
   *
   * @param controllerClass
   * @tparam A
   * @return
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val instance = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[User]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(MyRuntimeEnvironment)
    }
    instance.getOrElse(super.getControllerInstance(controllerClass))
  }

  object MyViewTemplates {

    /**
     * The default views.
     */
    class Default(env: RuntimeEnvironment[_]) extends ViewTemplates {
      implicit val implicitEnv = env

      override def getLoginPage(form: Form[(String, String)],
                                msg: Option[String] = None)
                               (implicit request: RequestHeader, lang: Lang): Html =
        views.html.login(null)

      override def getSignUpPage(form: Form[RegistrationInfo], token: String)
                                (implicit request: RequestHeader, lang: Lang): Html =
        Registration.signUp(form, token)

      override def getStartSignUpPage(form: Form[String])
                                     (implicit request: RequestHeader, lang: Lang): Html =
        Registration.startSignUp(form)

      override def getStartResetPasswordPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html =
        Registration.startResetPassword(form)


      override def getResetPasswordPage(form: Form[(String, String)], token: String)(implicit request: RequestHeader, lang: Lang): Html =
        Registration.resetPasswordPage(form, token)


      override def getPasswordChangePage(form: Form[ChangeInfo])(implicit request: RequestHeader, lang: Lang): Html =
        securesocial.views.html.passwordChange(form)


      def getNotAuthorizedPage(implicit request: RequestHeader, lang: Lang): Html =
        Html("no authorized")

    }

  }

  /** 9
    * The runtime environment for this sample app.
    */
  object MyRuntimeEnvironment extends RuntimeEnvironment.Default[User] {
    override lazy val routes = new CustomRoutesService()
    override lazy val userService: UserService[User] = createUserService()
    override lazy val eventListeners = List(new SecureSocialEventListener())
    //    override lazy val viewTemplates: ViewTemplates = new ViewTemplates.Default(this)
    override lazy val viewTemplates: ViewTemplates = new MyViewTemplates.Default(this)
    override lazy val providers = ListMap(
      include(new GitHubProvider(routes, cacheService, oauth2ClientFor(GitHubProvider.GitHub))))
    //      ,include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
    //      include(new UsernamePasswordProvider[DemoUser](userService, avatarService, viewTemplates, passwordHashers)))

    private def createUserService() = {
      val inMemoryDB: Boolean = Play.application.configuration.getBoolean("IN_MEMORY_DB", false)

      inMemoryDB match {
        case true => new InMemoryUserService
        case false => new SimpleDBUserService
      }

    }

  }

}
