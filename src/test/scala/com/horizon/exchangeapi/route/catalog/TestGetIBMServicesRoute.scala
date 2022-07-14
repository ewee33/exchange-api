package com.horizon.exchangeapi.route.catalog

import com.horizon.exchangeapi.{ApiTime, ApiUtils, GetServicesResponse, HttpCode, Password, Role, TestDBConnection}
import com.horizon.exchangeapi.tables.{AgbotRow, AgbotsTQ, NodeRow, NodesTQ, OrgRow, OrgsTQ, ResourceChangesTQ, Service, ServiceRef, ServiceRow, ServicesTQ, UserRow, UsersTQ}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods
import org.json4s.native.Serialization
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import scalaj.http.{Http, HttpResponse}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

class TestGetIBMServicesRoute extends AnyFunSuite with BeforeAndAfterAll {

  private val ACCEPT = ("Accept","application/json")
  private val AWAITDURATION: Duration = 15.seconds
  private val DBCONNECTION: TestDBConnection = new TestDBConnection
  private val URL = sys.env.getOrElse("EXCHANGE_URL_ROOT", "http://localhost:8080") + "/v1/catalog/services"

  private implicit val formats = DefaultFormats

  private val HUBADMINPASSWORD = "hubadminpassword"
  private val USERPASSWORD = "userpassword"
  private val NODETOKEN = "nodetoken"
  private val AGBOTTOKEN = "agbottoken"

  private val TESTORGS: Seq[OrgRow] =
    Seq(
      OrgRow(
        heartbeatIntervals = "",
        description        = "Test Organization 1",
        label              = "testGetIBMServices",
        lastUpdated        = ApiTime.nowUTC,
        limits             = "",
        orgId              = "TestGetIBMServicesRouteOrg1",
        orgType            = "testOrg",
        tags               = None
      )
    )

  private val TESTUSERS: Seq[UserRow] =
    Seq(
      UserRow(
        username    = "root/TestGetIBMServicesRouteHubAdmin",
        orgid       = "root",
        hashedPw    = Password.hash(HUBADMINPASSWORD),
        admin       = false,
        hubAdmin    = true,
        email       = "TestGetIBMServicesRouteHubAdmin@ibm.com",
        lastUpdated = ApiTime.nowUTC,
        updatedBy   = "root/root"
      ),
      UserRow(
        username    = TESTORGS(0).orgId + "/user",
        orgid       = TESTORGS(0).orgId,
        hashedPw    = Password.hash(USERPASSWORD),
        admin       = true,
        hubAdmin    = false,
        email       = "user@ibm.com",
        lastUpdated = ApiTime.nowUTC,
        updatedBy   = "root/root"
      )
    )

  private val TESTAGBOTS: Seq[AgbotRow] =
    Seq(
      AgbotRow(
        id = TESTORGS(0).orgId + "/agbot",
        orgid = TESTORGS(0).orgId,
        token = Password.hash(AGBOTTOKEN),
        name = "",
        owner = TESTUSERS(1).username, //org 1 user
        msgEndPoint = "",
        lastHeartbeat = ApiTime.nowUTC,
        publicKey = ""
      )
    )

  private val TESTNODES: Seq[NodeRow] =
    Seq(
      NodeRow(
        arch               = "",
        id                 = TESTORGS(0).orgId + "/node",
        heartbeatIntervals = "",
        lastHeartbeat      = None,
        lastUpdated        = ApiTime.nowUTC,
        msgEndPoint        = "",
        name               = "",
        nodeType           = "",
        orgid              = TESTORGS(0).orgId,
        owner              = TESTUSERS(1).username, //org 1 user
        pattern            = "",
        publicKey          = "",
        regServices        = "",
        softwareVersions   = "",
        token              = Password.hash(NODETOKEN),
        userInput          = ""
      )
    )

  private val TESTSERVICES: Seq[ServiceRow] =
    Seq(
      ServiceRow(
        service = "IBM/service1",
        orgid = "IBM",
        owner = TESTUSERS(0).username, //hub admin
        label = "service1 label",
        description = "service1 description",
        public = true,
        documentation = "service1 documentation",
        url = "service1 url",
        version = "1.0.0",
        arch = "service1 arch",
        sharable = "singleton",
        matchHardware = "{\"service1key1\":\"service1value\",\"service1key2\":1}",
        requiredServices = "[{\"url\":\"service1\",\"org\":\"service1\",\"version\":\"1.0.0\",\"versionRange\":\"[1.0.0,INFINITY)\",\"arch\":\"service1\"}]",
        userInput = "[{\"service1key\":\"service1value\"},{\"service1key\":\"service1value\"}]",
        deployment = "service1 deployment",
        deploymentSignature = "service1 deploymentSignature",
        clusterDeployment = "service1 clusterDeployment",
        clusterDeploymentSignature = "service1 clusterDeploymentSignature",
        imageStore = "{\"service1key1\":\"service1value\",\"service1key2\":1}",
        lastUpdated = ApiTime.nowUTC
      ),
      ServiceRow(
        service = "IBM/service2",
        orgid = "IBM",
        owner = "root/root",
        label = "service2 label", //
        description = "",
        public = true,
        documentation = "",
        url = "service2 url", //
        version = "1.0.0", //
        arch = "service2 arch", //
        sharable = "singleton", //
        matchHardware = "",
        requiredServices = "",
        userInput = "",
        deployment = "",
        deploymentSignature = "",
        clusterDeployment = "",
        clusterDeploymentSignature = "",
        imageStore = "",
        lastUpdated = ApiTime.nowUTC
      ),
      ServiceRow(
        service = TESTORGS(0).orgId + "/service3",
        orgid = TESTORGS(0).orgId,
        owner = TESTUSERS(1).username, //org 1 user
        label = "service3 label", //
        description = "",
        public = true,
        documentation = "",
        url = "service3 url", //
        version = "1.0.0", //
        arch = "service3 arch", //
        sharable = "singleton", //
        matchHardware = "",
        requiredServices = "",
        userInput = "",
        deployment = "",
        deploymentSignature = "",
        clusterDeployment = "",
        clusterDeploymentSignature = "",
        imageStore = "",
        lastUpdated = ApiTime.nowUTC
      ),
      ServiceRow(
        service = "IBM/service4",
        orgid = "IBM",
        owner = "root/root",
        label = "service4 label", //
        description = "",
        public = false,
        documentation = "",
        url = "service4 url", //
        version = "1.0.0", //
        arch = "service4 arch", //
        sharable = "singleton", //
        matchHardware = "",
        requiredServices = "",
        userInput = "",
        deployment = "",
        deploymentSignature = "",
        clusterDeployment = "",
        clusterDeploymentSignature = "",
        imageStore = "",
        lastUpdated = ApiTime.nowUTC
      )
    )

  private val ROOTAUTH = ("Authorization","Basic " + ApiUtils.encode(Role.superUser + ":" + sys.env.getOrElse("EXCHANGE_ROOTPW", "")))
  private val HUBADMINAUTH = ("Authorization", "Basic " + ApiUtils.encode(TESTUSERS(0).username + ":" + HUBADMINPASSWORD))
  private val USERAUTH = ("Authorization", "Basic " + ApiUtils.encode(TESTUSERS(1).username + ":" + USERPASSWORD))
  private val AGBOTAUTH = ("Authorization", "Basic " + ApiUtils.encode(TESTAGBOTS(0).id + ":" + AGBOTTOKEN))
  private val NODEAUTH = ("Authorization", "Basic " + ApiUtils.encode(TESTNODES(0).id + ":" + NODETOKEN))

  override def beforeAll(): Unit = {
    Await.ready(DBCONNECTION.getDb.run(
      (OrgsTQ ++= TESTORGS) andThen
      (UsersTQ ++= TESTUSERS) andThen
      (AgbotsTQ ++= TESTAGBOTS) andThen
      (NodesTQ ++= TESTNODES) andThen
      (ServicesTQ ++= TESTSERVICES)
    ), AWAITDURATION)
  }

  override def afterAll(): Unit = {
    Await.ready(DBCONNECTION.getDb.run(
      ResourceChangesTQ.filter(_.orgId startsWith "TestGetIBMServicesRoute").delete andThen
        OrgsTQ.filter(_.orgid startsWith "TestGetIBMServicesRoute").delete andThen
        UsersTQ.filter(_.username === TESTUSERS(0).username).delete andThen
        ServicesTQ.filter(_.orgid === "IBM").delete
    ), AWAITDURATION)
    DBCONNECTION.getDb.close()
  }

  def assertServicesEqual(s1: Service, s2: ServiceRow): Unit = {
    assert(s1.owner === s2.owner)
    assert(s1.label === s2.label)
    assert(s1.description === s2.description)
    assert(s1.public === s2.public)
    assert(s1.documentation === s2.documentation)
    assert(s1.url === s2.url)
    assert(s1.version === s2.version)
    assert(s1.arch === s2.arch)
    assert(s1.sharable === s2.sharable)
    assert(s1.matchHardware === Serialization.read[Map[String, Any]](s2.matchHardware))
    assert(s1.requiredServices === Serialization.read[List[ServiceRef]](s2.requiredServices))
    assert(s1.userInput === Serialization.read[List[Map[String, String]]](s2.userInput))
    assert(s1.deployment === s2.deployment)
    assert(s1.deploymentSignature === s2.deploymentSignature)
    assert(s1.clusterDeployment === s2.clusterDeployment)
    assert(s1.clusterDeploymentSignature === s2.clusterDeploymentSignature)
    assert(s1.imageStore === Serialization.read[Map[String, Any]](s2.imageStore))
    assert(s1.lastUpdated === s2.lastUpdated)
  }

  test("GET /catalog/services -- as root -- success") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(ROOTAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Service] = JsonMethods.parse(response.body).extract[GetServicesResponse].services
    assert(responseBody.size === 2)
    assert(responseBody.contains(TESTSERVICES(0).service))
    assertServicesEqual(responseBody(TESTSERVICES(0).service), TESTSERVICES(0))
    assert(responseBody.contains(TESTSERVICES(1).service))
    assertServicesEqual(responseBody(TESTSERVICES(1).service), TESTSERVICES(1))
  }

  test("GET /catalog/services?orgtype=testOrg -- success") {
    val response: HttpResponse[String] = Http(URL + "?orgtype=testOrg").headers(ACCEPT).headers(ROOTAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Service] = JsonMethods.parse(response.body).extract[GetServicesResponse].services
    assert(responseBody.size === 1)
    assert(responseBody.contains(TESTSERVICES(2).service))
    assertServicesEqual(responseBody(TESTSERVICES(2).service), TESTSERVICES(2))
  }

  test("GET /catalog/services?orgtype=doesNotExist -- 404 not found") {
    val response: HttpResponse[String] = Http(URL + "?orgtype=doesNotExist").headers(ACCEPT).headers(ROOTAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.NOT_FOUND.intValue)
    val responseBody: Map[String, Service] = JsonMethods.parse(response.body).extract[GetServicesResponse].services
    assert(responseBody.isEmpty)
  }

  test("GET /catalog/services -- as hub admin -- 403 access denied") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(HUBADMINAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.ACCESS_DENIED.intValue)
  }

  test("GET /catalog/services -- as user -- success") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(USERAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Service] = JsonMethods.parse(response.body).extract[GetServicesResponse].services
    assert(responseBody.size === 2)
    assert(responseBody.contains(TESTSERVICES(0).service))
    assertServicesEqual(responseBody(TESTSERVICES(0).service), TESTSERVICES(0))
    assert(responseBody.contains(TESTSERVICES(1).service))
    assertServicesEqual(responseBody(TESTSERVICES(1).service), TESTSERVICES(1))
  }

  test("GET /catalog/services -- as agbot -- success") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(AGBOTAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Service] = JsonMethods.parse(response.body).extract[GetServicesResponse].services
    assert(responseBody.size === 2)
    assert(responseBody.contains(TESTSERVICES(0).service))
    assertServicesEqual(responseBody(TESTSERVICES(0).service), TESTSERVICES(0))
    assert(responseBody.contains(TESTSERVICES(1).service))
    assertServicesEqual(responseBody(TESTSERVICES(1).service), TESTSERVICES(1))
  }

  test("GET /catalog/services -- as node -- success") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(NODEAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Service] = JsonMethods.parse(response.body).extract[GetServicesResponse].services
    assert(responseBody.size === 2)
    assert(responseBody.contains(TESTSERVICES(0).service))
    assertServicesEqual(responseBody(TESTSERVICES(0).service), TESTSERVICES(0))
    assert(responseBody.contains(TESTSERVICES(1).service))
    assertServicesEqual(responseBody(TESTSERVICES(1).service), TESTSERVICES(1))
  }

}
