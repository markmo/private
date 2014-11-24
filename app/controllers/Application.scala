package controllers

import java.io.StringReader

import play.api._
import play.api.mvc._
import services.DbService
import services.FileLoader
import services.Parser
import com.github.tototoshi.csv._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def obfuscate = Action { implicit request =>
    val sql = request.body.asText.getOrElse("")
    val parsed = Parser.parse(sql, obfuscate=true)(0)
    Ok(parsed)
  }

  def clarify = Action { implicit request =>
    val sql = request.body.asText.getOrElse("")
    val parsed = Parser.parse(sql, obfuscate=false)(0)
    Ok(parsed)
  }

  def loadFile(tableName: String) = Action { implicit request =>
    val reader = CSVReader.open(new StringReader(request.body.asText.getOrElse("")))
    FileLoader.loadFile(tableName, reader.toStream())
    Ok("Done")
  }

  def execute = Action { implicit request =>
    val sql = request.body.asText.getOrElse("")
    val parsed = Parser.parse(sql, obfuscate=true)(0)
    DbService.executeStatement(parsed)
    Ok("Done")
  }

  def query = Action { implicit request =>
    val sql = request.body.asText.getOrElse("")
    val parsed = Parser.parse(sql, obfuscate=true)(0)
    Ok(DbService.executeQuery(parsed).mkString("\n"))
  }

}