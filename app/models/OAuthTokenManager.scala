package models


import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import slick.driver.JdbcProfile
import models.dao.PortableJodaSupport._
import driver.api._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Play
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import utils.StringUtils

case class OAuthToken(
    id:            String           = StringUtils.generateUuid,
    created_at:    DateTime         = DateTime.now(DateTimeZone.UTC),
    deleted_at:    Option[DateTime] = Some(DateTime.now(DateTimeZone.UTC).plusDays(1)),
    user_id:   String,
    client_id:     String,
    access_token:  String           = StringUtils.generateUuid,
    refresh_token: String           = StringUtils.generateUuid,
    code:          Option[String]   = Some(StringUtils.generateUuid),
    scope:         Option[String]   = None
)

class OAuthTokenTable(tag: Tag) extends Table[OAuthToken](tag, "oauth_tokens") {
  val id = column[String]("id", O.PrimaryKey)
  val created_at = column[DateTime]("created_at")
  val deleted_at = column[Option[DateTime]]("deleted_at")
  val user_id = column[String]("user_id")
  val client_id = column[String]("client_id")
  val access_token = column[String]("access_token")
  val refresh_token = column[String]("refresh_token")
  val code = column[Option[String]]("code")
  val scope = column[Option[String]]("scope")

  def * = (id, created_at, deleted_at, user_id, client_id, access_token, refresh_token, code, scope) <> ((OAuthToken.apply _).tupled, OAuthToken.unapply _)
}

class OAuthTokens extends HasDatabaseConfig[JdbcProfile] {

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val tables = TableQuery[OAuthTokenTable]

  private[this] val basic = tables.filter(_.deleted_at > DateTime.now(DateTimeZone.UTC))

  //---------------
  //Methods
  //---------------

  def findByConsumerAndClient(user_id: String, client_id: String): Future[Option[OAuthToken]] = {
    db.run(
      basic
      .filter(_.client_id === client_id)
      .filter(_.user_id === user_id)
      .result
      .headOption
    )
  }

  def findByToken(token: String): Future[Option[OAuthToken]] = {
    db.run(basic.filter(_.access_token === token).result.headOption)
  }

  def findByCode(code: String): Future[Option[OAuthToken]] = {
    db.run(basic.filter(_.code === code).result.headOption)
  }

  def findByRefreshToken(refresh_token: String): Future[Option[OAuthToken]] = {
    db.run(basic.filter(_.refresh_token === refresh_token).result.headOption)
  }

  def markCodeUsed(code: String): Future[Int] = {
    val q = basic.filter(_.code === code).map(_.code).update(None)
    db.run(q)
  }

  def create(token:OAuthToken):Future[OAuthToken] = {
    db.run(tables += token) map (_ => token)
  }

  def delete(id:String):Future[Int] = {
    db.run(basic.filter(_.id === id).delete)
  }

}

trait OAuthTokensAware {
  implicit lazy val accessTokenDao: OAuthTokens = new OAuthTokens()
}
