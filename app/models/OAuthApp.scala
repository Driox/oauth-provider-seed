package models


import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig
import slick.driver.JdbcProfile
import models.dao.PortableJodaSupport._
import driver.api._
import dao._
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Play
import play.api.Play.current

import scala.concurrent.Future

import utils.StringUtils

case class OAuthApp(
    id:            String           = StringUtils.generateUuid,
    created_at:    DateTime         = DateTime.now(DateTimeZone.UTC),
    deleted_at:    Option[DateTime] = None,
    client_id:     String           = StringUtils.generateUuid,
    client_secret: String           = StringUtils.generateUuid,
    name:          String,
    redirect_url:  Option[String]   = None
)

class OAuthAppTable(tag: Tag) extends Table[OAuthApp](tag, "oauth_app") {
  val id = column[String]("id", O.PrimaryKey)
  val created_at = column[DateTime]("created_at")
  val deleted_at = column[Option[DateTime]]("deleted_at")
  val client_id = column[String]("client_id")
  val client_secret = column[String]("client_secret")
  val name = column[String]("name")
  val redirect_url = column[Option[String]]("redirect_url")

  def * = (id, created_at, deleted_at, client_id, client_secret, name, redirect_url) <> ((OAuthApp.apply _).tupled, OAuthApp.unapply _)
}

class OAuthApps extends HasDatabaseConfig[JdbcProfile] {

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val tables = TableQuery[OAuthAppTable]

  //---------------
  //Methods
  //---------------

  def findByClientId(client_id: String): Future[Option[OAuthApp]] = {
    db.run(tables.filter(_.client_id === client_id).result.headOption)
  }

}

trait OAuthAppsAware {
  implicit lazy val appManagerDao: OAuthApps = new OAuthApps()
}
