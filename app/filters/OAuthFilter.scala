package filters

import helpers._
import helpers.SorusDSL._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future
import scalaz.\/

/**
 * This filter check OAuth2 token and if present it gather the header for the next filter
 */
object OAuthFilter extends Filter with Sorus with OAuthTokensAware with OAuthAppsAware {

  private val userService = new UserService()

  def apply(nextFilter: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {

    // For now OAuth is only for read only endpoint aka GET endpoint
    if (request.method != "GET") {
      nextFilter(request)
    } else {
      request.path.matches("^/v[0-9]*/.*") match {
        case false => nextFilter(request)
        case true => {
          isAllowed(request).flatMap(_.map { user =>
            nextFilter(decorate(request, user))
          }.getOrElse {
            nextFilter(request)
          })
        }
      }
    }
  }

  private[this] def oauthToken(request: RequestHeader): Option[String] = {
    request.headers.get("Authorization").map { k =>
      if (k.startsWith("Bearer ")) {
        k.substring(7)
      } else {
        k
      }
    }
  }

  private[this] def isAllowed(request: RequestHeader): Future[Option[User]] = {
    val rez: Future[Fail \/ User] = for {
      access_token <- oauthToken(request) ?| ()
      token <- accessTokenDao.findByToken(access_token) ?| ()
      user <- userService.loadById(token.user_id) ?| ()
    } yield {
      user
    }

    rez.map(_.toOption)
  }

  private[this] def decorate(request: RequestHeader, user: User): RequestHeader = {
    val additionalKeys:Map[String, String] = Map(
      "Authorization" -> ("Basic " + user.email + ":" + user.password)
    )

    val headers = new Headers(
      headers = request.headers.toSimpleMap.filterKeys(k => k != "Authorization").toSeq ++ additionalKeys
    )

    request.copy(headers = headers)
  }

}
