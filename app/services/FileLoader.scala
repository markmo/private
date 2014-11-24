package services

import com.redis.RedisClient

import scala.util.Random

/**
 * Created by markmo on 4/11/2014.
 */
object FileLoader {

  val r = new RedisClient("localhost", 6379)
  val random = new Random

  def loadFile(tableName: String, in: Stream[List[String]]) = {
    var firstRow = true
    var header: List[String] = null
    for (row <- in) {
      if (firstRow) {
        header = row
        firstRow = false
      } else {
        val values = for ((column, i) <- row.zipWithIndex) yield {
          val columnName = header(i)
          val columnKey = s"$tableName.$columnName"
          val iskey = r.hget(columnKey.toLowerCase, "iskey").getOrElse("F") == "T"
          val dataType = r.hget(columnKey.toLowerCase, "dataType").getOrElse("")
          val width = r.hget(columnKey.toLowerCase, "width").getOrElse("0").toInt
          if (iskey) {
            s"'${getRandomizedKey(s"$tableName.$columnName", width)}'"
          } else {
            row(i)
          }
        }
        val sql = s"INSERT INTO $tableName(${header.mkString(",")}) VALUES(${values.mkString(",")});"
        println(sql)
        val parsed = Parser.parse(sql, obfuscate = true)(0)
        println(parsed)
        DbService.executeStatement(parsed)
      }
    }
  }

  def getRandomizedKey(columnName: String, width: Int): String = {
    val bytes: Array[Byte] = Array.fill[Byte](width)(0)
    random.nextBytes(bytes)
    val key = bytes.toString
    if (r.sismember(s"$columnName:keys", key)) {
      getRandomizedKey(columnName, width)
    } else {
      r.sadd(s"$columnName:keys", key)
      key
    }
  }

}
