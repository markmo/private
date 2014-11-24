package services

import anorm._
import play.api.db.DB
import play.api.Play.current

/**
 * Created by markmo on 4/11/2014.
 */
object DbService {

  def executeStatement(sql: String) =
    DB.withConnection { implicit c =>
      val result: Boolean = SQL(sql).execute()
    }

  def executeQuery(sql: String) =
    DB.withConnection { implicit c =>
      val select = SQL(sql)
      select().map(row =>
        row.asList.mkString(",")
      ).toList
    }

}
