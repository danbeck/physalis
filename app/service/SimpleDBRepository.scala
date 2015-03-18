package service

import models.User
import awscala._
import org.slf4j.LoggerFactory
import play.api.Logger
import models.Project
import awscala.simpledb.SimpleDBClient
import awscala.simpledb.Domain
import awscala.simpledb.Item
//_, simpledb._

object SimpleDBRepository {

  implicit val simpleDB = new SimpleDBClient()
  val domain: Domain = simpleDB.createDomain("buildTasks")

  def save(project: Project) = {
    Logger.info(s"SimpleDB: Adding project '${project.id}' '${project.name}' '${project.gitUrl}'")
    domain.put(project.id,
      "gitUrl" -> project.gitUrl,
      "user" -> project.user.username.get,
      "project" -> project.name,
      "state" -> "NEW",
      "s3Url" -> "")

  }
  def saveUser(user: User) = {
    Logger.info("creating simpledb")
    implicit val simpleDB = new SimpleDBClient()
    //    val region: Region = new Region()
    Logger.info("creating domain")
    val domain: Domain = simpleDB.createDomain("users")
    domain.put("daniel", "name" -> "Daniel Beck", "email" -> "d.danielbeck@googlemail.com")
    domain.put("karin", "name" -> "Karin Beck", "email" -> "kbeck@googlemail.com")

    val items: Seq[Item] = domain.select(s"select * from users where name = 'Daniel Beck'")

    Logger.info("items: " + items)
    Logger.error("items: " + items)
    simpleDB.domains.foreach(_.destroy())

    //    val awsCreditential = new EnvironmentVariableCredentialsProvider().getCredentials()
    //    val client = new AmazonSimpleDBClient(awsCreditential)
    //    val userDomain = "user"
    //    val createDomainRequest = new CreateDomainRequest(userDomain)
    //    client.createDomain(createDomainRequest);
    //    
    //    val users = List (new ReplaceableItem().withName("daniel").withAttributes(arg0))
    //    ReplaceableItem
    //    //    client.
    //    Ok("connection was ok")

  }
}