package controllers

import controllers.ActionDSL._
import models._

import play.api.mvc._
import play.api.libs.json._

import scala.concurrent._

class OAuth2Controller extends Controller with MonadicActions with OAuthTokensAware with OAuthAppsAware {

  private val userService = new UserService()

  def displayAuthorize() = Action.async { implicit request =>
    for {
      client_id <- params("client_id") ?| BadRequest("Missing parameters client_id")
      redirect_uri <- params("redirect_uri") ?| BadRequest("Missing parameters redirect_uri")
      client_app <- appManagerDao.findByClientId(client_id) ?| NotFound("No application registered for this key")
      user <- findConsumer(request) ?| Redirect(login_url)
    } yield {
      val scope = params("scope")
      Ok(views.html.authorize(client_app, user.id, redirect_uri, scope))
    }
  }

  def authorize() = Action.async { implicit request =>
    for {
      client_id <- params("client_id") ?| BadRequest("Missing parameters client_id")
      user_id <- params("user_id") ?| BadRequest("Missing parameters user_id")
      scope <- params("scope") ?| BadRequest("No scope allowed")
      client_app <- appManagerDao.findByClientId(client_id) ?| NotFound("No application registered for this key")
      token <- accessTokenDao.create(OAuthToken(
        user_id = user_id,
        client_id = client_id
      )) ?| BadRequest("Cannot create access code")
    } yield {
      val url = params("redirect_uri")
        .orElse(client_app.redirect_url)
        .getOrElse(login_url)
      Redirect(url + s"?code=${token.code.getOrElse("")}")
    }
  }

  def access() = Action.async { implicit request =>

    for {
      code <- params("code") ?| BadRequest("Missing parameters code")
      client_id <- params("client_id") ?| BadRequest("Missing parameters client_id")
      client_secret <- params("client_secret") ?| BadRequest("Missing parameters client_secret")
      client_app <- appManagerDao.findByClientId(client_id) ?| NotFound("No application registered for this key")
      _ <- (client_app.client_secret == client_secret) ?| NotFound("No application registered for this key")
      token <- accessTokenDao.findByCode(code) ?| NotFound("No code for this client")
      _ <- (client_id == token.client_id) ?| NotFound("No code for this client")
    } yield {
      accessTokenDao.markCodeUsed(code)
      Ok(Json.toJson(OAuthResponse(token.access_token, token.refresh_token)))
    }
  }

  def refresh() = Action.async { implicit request =>
    for {
      refresh_token <- params("refresh_token") ?| BadRequest("Missing parameters refresh_token")
      client_id <- params("client_id") ?| BadRequest("Missing parameters client_id")
      user_id <- params("user_id") ?| BadRequest("Missing parameters user_id")
      client_app <- appManagerDao.findByClientId(client_id) ?| NotFound("No application registered for this key")
      old_token <- accessTokenDao.findByRefreshToken(refresh_token) ?| NotFound("No code for this client")
      _ <- (client_id == old_token.client_id) ?| NotFound("No code for this client")
      _ <- (user_id == old_token.user_id) ?| NotFound("No code for this client")
      new_token <- accessTokenDao.create(OAuthToken(
        user_id = old_token.user_id,
        client_id = old_token.client_id,
        code = None,
        scope = old_token.scope
      )) ?| BadRequest("Cannot create access code")
    } yield {
      accessTokenDao.delete(old_token.id)
      Ok(Json.toJson(OAuthResponse(new_token.access_token, new_token.refresh_token)))
    }
  }

  private[this] val login_url = "https://setup.particeep.com/login"
  private[this] val signin_url = "https://setup.particeep.com/signin"

  private[this] case class OAuthResponse(access_token: String, refresh_token: String)
  private[this] implicit val OAuthResponseFormat = Json.format[OAuthResponse]

  private[this] def params(name: String)(implicit request: Request[AnyContent]): Option[String] = {
    request.getQueryString(name).orElse(
      request.body.asFormUrlEncoded.flatMap(_.get(name).flatMap(_.headOption))
    )
  }

  private[this] def findConsumer(request: Request[AnyContent]): Future[Option[User]] = {
    val email = request.session.get("userEmail").getOrElse("")
    userService.findByEmail(email)
  }

}
