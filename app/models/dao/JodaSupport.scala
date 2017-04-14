package models.dao

import play.api.Play
import play.api.Play.current

/**
 * This is needed to support joda time for all driver
 *
 * https://github.com/tototoshi/slick-joda-mapper/issues/8
 */
object PortableJodaSupport
  extends com.github.tototoshi.slick.GenericJodaSupport(play.api.db.slick.DatabaseConfigProvider.get.driver)
