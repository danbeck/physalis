package controllers

import play.api.mvc._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.http.HeaderNames
import scala.concurrent.{ ExecutionContext, Future }
import play.twirl.api.Html
import securesocial.core.utils._
import securesocial.core.authenticator._
import play.api.mvc.Result
import securesocial.core.RuntimeEnvironment
import securesocial.core.authenticator.Authenticator
import securesocial.core.Authorization
import models.User

/**
 * Provides the actions that can be used to protect controllers and retrieve the current user
 * if available.
 *
 */
trait PhysalisSecureSocial extends securesocial.core.SecureSocial[User] {

  /**
   * An action that adds the current user in the request if it's available.
   */
  object PhysalisUserAwareAction extends UserAwareActionBuilder {
    def apply[A]() = new PhysalisUserAwareActionBuilder()
  }

  /**
   * The UserAwareAction builder
   */
  class PhysalisUserAwareActionBuilder extends ActionBuilder[({ type R[A] = RequestWithUser[A] })#R] {
    override protected implicit def executionContext: ExecutionContext = env.executionContext

    override def invokeBlock[A](request: Request[A],
                                block: (RequestWithUser[A]) => Future[Result]): Future[Result] =
      {
        env.authenticatorService.fromRequest(request).flatMap {
          case Some(authenticator) if authenticator.isValid =>
            authenticator.touch.flatMap {
              a => block(RequestWithUser(Some(a.user), Some(a), request))
            }
          case Some(authenticator) if !authenticator.isValid =>
            block(RequestWithUser(None, None, request)).flatMap(_.discardingAuthenticator(authenticator))
          case None =>
            block(RequestWithUser(None, None, request))
        }
      }
  }
}
