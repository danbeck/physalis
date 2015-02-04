package service

import models.User
import awscala._, simpledb._

import org.slf4j.LoggerFactory
//_, simpledb._

object SimpleDBRepository {

  val log = LoggerFactory.getLogger(this.getClass)

  def saveUser(user: User) = {
    log.info("creating simpledb")
    implicit val simpleDB = new SimpleDBClient()
    //    val region: Region = new Region()
    log.info("creating domain")
    val domain: Domain = simpleDB.createDomain("users")
    domain.put("daniel", "name" -> "Daniel Beck", "email" -> "d.danielbeck@googlemail.com")
    domain.put("karin", "name" -> "Karin Beck", "email" -> "kbeck@googlemail.com")

    val items: Seq[Item] = domain.select(s"select * from users where name = 'Daniel Beck'")

    log.info("items: " + items)
    log.error("items: " +items )
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