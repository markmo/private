package services

import com.foundationdb.sql.parser._
import com.foundationdb.sql.parser.NodeTypes._

import services.{CryptoService => cs}

/**
 * Created by markmo on 4/11/2014.
 */
class QueryTreeVisitor(obfuscate: Boolean = true) extends Visitor {

  var isDefine = false
  var isSelect = false
  var baseTableMetadata: TableMetadata = null
  var tableMetadata: TableMetadata = null

  var columns: List[ColumnMetadata] = Nil

  def getMetadata: Option[StatementMetadata] = {
    if (columns.isEmpty) {
      return None
    }
    if (baseTableMetadata == null) {
      baseTableMetadata = tableMetadata
    }
    Some(StatementMetadata(baseTableMetadata, columns))
  }

  override def visit(visitable: Visitable): Visitable = {
    val node = visitable.asInstanceOf[QueryTreeNode]

    println(node.getNodeType)

    node.getNodeType match {

      case SELECT_NODE => isSelect = true

      case CREATE_TABLE_NODE =>
        val ref = node.asInstanceOf[CreateTableNode]
        val schema = ref.getObjectName.getSchemaName
        val tableName = ref.getObjectName.getTableName
        val newTableName = if (obfuscate) {
          cs.obfuscate(tableName)
        } else {
          cs.clarify(tableName)
        }
        isDefine = true
        if (obfuscate) {
          tableMetadata = TableMetadata(schema, tableName, newTableName)
        }
        ref.getObjectName.init(schema, newTableName)

      case COLUMN_DEFINITION_NODE =>
        val ref = node.asInstanceOf[ColumnDefinitionNode]
        val dataType = ref.getType
        val dataTypeName = dataType.getTypeName
        val width = dataType.getMaximumWidth
        val autoIncrementInfo = Array(
          ref.getAutoincrementStart,
          ref.getAutoincrementIncrement,
          ref.getAutoinc_create_or_modify_Start_Increment,
          if (ref.isAutoincrementColumn) 0 else 1
        )
        val columnName = ref.getColumnName
        val newColumnName = if (obfuscate) {
          cs.obfuscate(columnName)
        } else {
          cs.clarify(columnName)
        }
        if (obfuscate && isDefine) {
          columns = columns :+ ColumnMetadata(tableMetadata, columnName, newColumnName, dataTypeName, width)
        }
        ref.init(newColumnName, ref.getDefaultNode, ref.getType, autoIncrementInfo)

      case FROM_BASE_TABLE =>
        val ref = node.asInstanceOf[FromBaseTable]
        val schema = ref.getTableName.getSchemaName
        val tableName = ref.getTableName.getTableName
        val newTableName = if (obfuscate) {
          cs.obfuscate(tableName)
        } else {
          cs.clarify(tableName)
        }
        if (obfuscate && isDefine) {
          tableMetadata = TableMetadata(schema, tableName, newTableName)
          baseTableMetadata = tableMetadata
        }
        ref.getTableName.init(schema, newTableName)

      case TABLE_NAME =>
        val ref = node.asInstanceOf[TableName]
        val schema = ref.getSchemaName
        val tableName = ref.getTableName
//        if (isSelect && tableName == baseTableMetadata.tableName) {
          val newTableName = if (obfuscate) {
            cs.obfuscate(tableName)
          } else {
            cs.clarify(tableName)
          }
          if (obfuscate && isDefine) {
            tableMetadata = TableMetadata(schema, tableName, newTableName)
          }
          ref.init(schema, newTableName)
//        }

      case COLUMN_REFERENCE =>
        val ref = node.asInstanceOf[ColumnReference]
        val columnName = ref.getColumnName
        val newColumnName = if (obfuscate) {
          cs.obfuscate(columnName)
        } else {
          cs.clarify(columnName)
        }
        if (obfuscate && isDefine) {
          val dataType = ref.getType
          val dataTypeName = dataType.getTypeName
          val width = dataType.getMaximumWidth
          columns = columns :+ ColumnMetadata(tableMetadata, columnName, newColumnName, dataTypeName, width)
        }
        ref.init(newColumnName, ref.getTableName)

      case _ =>
    }

    visitable
  }

  override def stopTraversal(): Boolean = false

  override def skipChildren(visitable: Visitable): Boolean = false

  override def visitChildrenFirst(visitable: Visitable): Boolean = false

}

case class TableMetadata(schemaName: String, tableName: String, obfuscatedTableName: String) {

  def qualifiedName = schemaName + "." + tableName

}

case class ColumnMetadata(tableMetadata: TableMetadata, columnName: String, obfuscatedColumnName: String,
                          dataType: String, width: Int)

case class StatementMetadata(baseTableMetadata: TableMetadata, columns: List[ColumnMetadata])
