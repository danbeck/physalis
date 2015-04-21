package controllers

import play.api.mvc._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.http.HeaderNames
import scala.concurrent.{ExecutionContext, Future}
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
  object PhysalisUserAwareAction extends PhysalisUserAwareActionBuilder {
    def apply[A]() = new PhysalisUserAwareActionBuilder()
  }

  /**
   * The UserAwareAction builder
   */
  class PhysalisUserAwareActionBuilder extends ActionBuilder[({type R[A] = RequestWithUser[A]})#R] {
    override protected implicit def executionContext: ExecutionContext = env.executionContext

    override def invokeBlock[A](request: Request[A],
                                block: (RequestWithUser[A]) => Future[Result]): Future[Result] = {
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

//
//  /**
//   * A secured action.  If there is no user in the session the request is redirected
//   * to the login page
//   */
//  object PhysalisSecuredAction extends PhysalisSecuredActionBuilder {
//    /**
//     * Creates a secured action
//     */
//    def apply[A]() = new PhysalisSecuredActionBuilder(None)
//
//    /**
//     * Creates a secured action
//     * @param authorize an Authorize object that checks if the user is authorized to invoke the action
//     */
//    def apply[A](authorize: Authorization[U]) = new PhysalisSecuredActionBuilder(Some(authorize))
//  }
//
//  /**
//   * A builder for secured actions
//   *
//   * @param authorize an Authorize object that checks if the user is authorized to invoke the action
//   */
//  class PhysalisSecuredActionBuilder(authorize: Option[Authorization[U]] = None)
//    extends ActionBuilder[({type R[A] = SecuredRequest[A]})#R] {
//
//    override protected implicit def executionContext: ExecutionContext = env.executionContext
//
//    private val logger = play.api.Logger("securesocial.core.SecuredActionBuilder")
//
//    def invokeSecuredBlock[A](authorize: Option[Authorization[U]], request: Request[A],
//                              block: SecuredRequest[A] => Future[Result]): Future[Result] = {
//      env.authenticatorService.fromRequest(request).flatMap {
//        case Some(authenticator) if authenticator.isValid =>
//          authenticator.touch.flatMap { updatedAuthenticator =>
//            val user = updatedAuthenticator.user
//            if (authorize.isEmpty || authorize.get.isAuthorized(user, request)) {
//              block(SecuredRequest(user, updatedAuthenticator, request)).flatMap {
//                _.touchingAuthenticator(updatedAuthenticator)
//              }
//            } else {
//              notAuthorizedResult(request)
//            }
//          }
//        case Some(authenticator) if !authenticator.isValid =>
//          logger.debug("[securesocial] user tried to access with invalid authenticator : '%s'".format(request.uri))
//          notAuthenticatedResult(request).flatMap {
//            _.discardingAuthenticator(authenticator)
//          }
//        case None =>
//          logger.debug("[securesocial] anonymous user trying to access : '%s'".format(request.uri))
//          notAuthenticatedResult(request)
//      }
//    }
//
//    override def invokeBlock[A](request: Request[A],
//                                block: (SecuredRequest[A]) => Future[Result]): Future[Result] = {
//      invokeSecuredBlock(authorize, request, block)
//    }
//  }

}
