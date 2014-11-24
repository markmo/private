package services

import com.foundationdb.sql.parser.{StatementNode, SQLParser}
import java.util.{List => JList}
import com.foundationdb.sql.unparser.NodeToString
import com.redis.RedisClient

import scala.collection.JavaConversions._

/**
 * Created by markmo on 4/11/2014.
 */
object Parser {

  val r = new RedisClient("localhost", 6379)

  def parse(sql: String, obfuscate: Boolean = true) = {
    val parser = new SQLParser
    val composer = new NodeToString
    val nodes: JList[StatementNode] = parser.parseStatements(sql)
    for (node: StatementNode <- nodes) yield {
      val visitor = new QueryTreeVisitor(obfuscate)
      node.accept(visitor)
      updateMetadata(visitor.getMetadata)
      composer.toString(node)
    }
  }

  def updateMetadata(metadata: Option[StatementMetadata]) = {
    if (metadata.isDefined) {
      println(metadata)
      metadata.get.columns foreach { column =>
        r.sadd("tables", column.tableMetadata.qualifiedName)
        r.sadd(column.tableMetadata.qualifiedName + ":columns", column.columnName)
        r.hset(column.tableMetadata.qualifiedName, "obfuscatedTableName", column.tableMetadata.obfuscatedTableName)
        r.hset(column.tableMetadata.qualifiedName + "." + column.columnName, "obfuscatedColumnName", column.obfuscatedColumnName)
        r.hset(column.tableMetadata.qualifiedName + "." + column.columnName, "dataType", column.dataType)
        r.hset(column.tableMetadata.qualifiedName + "." + column.columnName, "width", column.width)
      }
    }
  }

}
