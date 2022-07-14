package com.horizon.exchangeapi.route.catalog

import com.horizon.exchangeapi.{ApiTime, ApiUtils, GetPatternsResponse, GetServicesResponse, HttpCode, Password, Role, TestDBConnection}
import com.horizon.exchangeapi.tables.{AgbotRow, AgbotsTQ, NodeRow, NodesTQ, OneSecretBindingService, OneUserInputService, OneUserInputValue, OrgRow, OrgsTQ, PServiceVersions, PServices, Pattern, PatternRow, PatternsTQ, ResourceChangesTQ, Service, ServiceRef, ServiceRow, ServicesTQ, UserRow, UsersTQ}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods
import org.json4s.native.Serialization
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import scalaj.http.{Http, HttpResponse}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}

class TestGetIBMPatternsRoute extends AnyFunSuite with BeforeAndAfterAll {

  private val ACCEPT = ("Accept","application/json")
  private val AWAITDURATION: Duration = 15.seconds
  private val DBCONNECTION: TestDBConnection = new TestDBConnection
  private val URL = sys.env.getOrElse("EXCHANGE_URL_ROOT", "http://localhost:8080") + "/v1/catalog/patterns"

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
        label              = "testGetIBMPatterns",
        lastUpdated        = ApiTime.nowUTC,
        limits             = "",
        orgId              = "TestGetIBMPatternsRouteOrg1",
        orgType            = "testOrg",
        tags               = None
      )
    )

  private val TESTUSERS: Seq[UserRow] =
    Seq(
      UserRow(
        username    = "root/TestGetIBMPatternsRouteHubAdmin",
        orgid       = "root",
        hashedPw    = Password.hash(HUBADMINPASSWORD),
        admin       = false,
        hubAdmin    = true,
        email       = "TestGetIBMPatternsRouteHubAdmin@ibm.com",
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

  private val TESTPATTERNS: Seq[PatternRow] = Seq(
    PatternRow(
      pattern = "IBM/pattern1",
      orgid = "IBM",
      owner = TESTUSERS(0).username,
      label = "pattern1 label",
      description = "pattern1 description",
      public = true,
      services = Serialization.write(
        List(
          PServices(
            serviceUrl = "pattern1_service",
            serviceOrgid = "IBM",
            serviceArch = "pattern1_service arch",
            agreementLess = Some(false),
            serviceVersions = List(
              PServiceVersions(
                version = "1.0.0",
                deployment_overrides = Some("string"),
                deployment_overrides_signature = Some("string"),
                priority = Some(
                  Map(
                  "priority_value" -> 50,
                  "retries" -> 1,
                  "retry_durations" -> 3600,
                  "verified_durations" -> 52
                  )
                ),
                upgradePolicy = Some(
                  Map(
                  "lifecycle" -> "immediate",
                  "time" -> "01:00AM"
                  )
                )
              )
            ),
            dataVerification = Some(
              Map(
                "metering" -> Map(
                  "tokens" -> 1,
                  "per_time_unit" -> "min",
                  "notification_interval" -> 30
                ),
                "URL" -> "data_verification_url",
                "enabled" -> true,
                "interval" -> 240,
                "check_rate" -> 15,
                "user" -> TESTUSERS(1).username,
                "password" -> USERPASSWORD
              )
            ),
            nodeHealth = Some(
              Map(
                "missing_heartbeat_interval" -> 600,
                "check_agreement_status" -> 120
              )
            )
          )
        )
      ),
      userInput = Serialization.write(
        List(
          OneUserInputService(
            serviceOrgid = TESTORGS(0).orgId,
            serviceUrl = "pattern1_service",
            serviceArch = Some("pattern1_service arch"),
            serviceVersionRange = Some("[1.0.0,INFINITY)"),
            inputs = List(
              OneUserInputValue(name = "key1", value = "value1"),
              OneUserInputValue(name = "key1", value = 5)
            )
          )
        )
      ),
      secretBinding = Serialization.write(
        List(
          OneSecretBindingService(
            serviceOrgid = TESTORGS(0).orgId,
            serviceUrl = "pattern1_service",
            serviceArch = Some("pattern1_service arch"),
            serviceVersionRange = Some("[1.0.0,INFINITY)"),
            secrets = List(
              Map("key1" -> "value1"),
              Map("key1" -> "value1")
            )
          )
        )
      ),
      agreementProtocols = "[{\"name\":\"Basic\"},{\"key\":\"value\"}]",
      lastUpdated = ApiTime.nowUTC
    ),
    PatternRow(
      pattern = "IBM/pattern2",
      orgid = "IBM",
      owner = "root/root",
      label = "pattern2 label",
      description = "pattern2 description",
      public = true,
      services = "",
      userInput = "",
      secretBinding = "",
      agreementProtocols = "",
      lastUpdated = ApiTime.nowUTC
    ),
    PatternRow(
      pattern = TESTORGS(0).orgId + "/pattern3",
      orgid = TESTORGS(0).orgId,
      owner = TESTUSERS(1).username,
      label = "pattern3 label",
      description = "pattern3 description",
      public = true,
      services = "",
      userInput = "",
      secretBinding = "",
      agreementProtocols = "",
      lastUpdated = ApiTime.nowUTC
    ),
    PatternRow(
      pattern = "IBM/pattern4",
      orgid = "IBM",
      owner = "root/root",
      label = "pattern4 label",
      description = "pattern4 description",
      public = false,
      services = "",
      userInput = "",
      secretBinding = "",
      agreementProtocols = "",
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
      (PatternsTQ ++= TESTPATTERNS)
    ), AWAITDURATION)
  }

  override def afterAll(): Unit = {
    Await.ready(DBCONNECTION.getDb.run(
      ResourceChangesTQ.filter(_.orgId startsWith "TestGetIBMPatternsRoute").delete andThen
        OrgsTQ.filter(_.orgid startsWith "TestGetIBMPatternsRoute").delete andThen
        UsersTQ.filter(_.username === TESTUSERS(0).username).delete andThen
        PatternsTQ.filter(_.orgid === "IBM").delete
    ), AWAITDURATION)
    DBCONNECTION.getDb.close()
  }

  def assertPatternsEqual(p1: Pattern, p2: PatternRow): Unit = {
    assert(p1.owner === p2.owner)
    assert(p1.label === p2.label)
    assert(p1.description === p2.description)
    assert(p1.public === p2.public)
    assert(p1.services === Serialization.read[List[PServices]](p2.services))
    assert(p1.userInput === Serialization.read[List[OneUserInputService]](p2.userInput))
    assert(p1.secretBinding === Serialization.read[List[OneSecretBindingService]](p2.secretBinding))
    assert(p1.agreementProtocols === Serialization.read[List[Map[String,String]]](p2.agreementProtocols))
    assert(p1.lastUpdated === p2.lastUpdated)
  }

  test("GET /catalog/patterns -- as root -- success") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(ROOTAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Pattern] = JsonMethods.parse(response.body).extract[GetPatternsResponse].patterns
    assert(responseBody.size === 2)
    assert(responseBody.contains(TESTPATTERNS(0).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(0).pattern), TESTPATTERNS(0))
    assert(responseBody.contains(TESTPATTERNS(1).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(1).pattern), TESTPATTERNS(1))
  }

  test("GET /catalog/patterns?orgtype=testOrg -- success") {
    val response: HttpResponse[String] = Http(URL + "?orgtype=testOrg").headers(ACCEPT).headers(ROOTAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Pattern] = JsonMethods.parse(response.body).extract[GetPatternsResponse].patterns
    assert(responseBody.size === 1)
    assert(responseBody.contains(TESTPATTERNS(2).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(2).pattern), TESTPATTERNS(2))
  }

  test("GET /catalog/patterns?orgtype=doesNotExist -- 404 not found") {
    val response: HttpResponse[String] = Http(URL + "?orgtype=doesNotExist").headers(ACCEPT).headers(ROOTAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.NOT_FOUND.intValue)
    val responseBody: Map[String, Pattern] = JsonMethods.parse(response.body).extract[GetPatternsResponse].patterns
    assert(responseBody.isEmpty)
  }

  test("GET /catalog/patterns -- as hub admin -- 403 access denied") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(HUBADMINAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.ACCESS_DENIED.intValue)
  }

  test("GET /catalog/patterns -- as user -- success") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(USERAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Pattern] = JsonMethods.parse(response.body).extract[GetPatternsResponse].patterns
    assert(responseBody.size === 2)
    assert(responseBody.contains(TESTPATTERNS(0).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(0).pattern), TESTPATTERNS(0))
    assert(responseBody.contains(TESTPATTERNS(1).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(1).pattern), TESTPATTERNS(1))
  }

  test("GET /catalog/patterns -- as agbot -- success") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(AGBOTAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Pattern] = JsonMethods.parse(response.body).extract[GetPatternsResponse].patterns
    assert(responseBody.size === 2)
    assert(responseBody.contains(TESTPATTERNS(0).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(0).pattern), TESTPATTERNS(0))
    assert(responseBody.contains(TESTPATTERNS(1).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(1).pattern), TESTPATTERNS(1))
  }

  test("GET /catalog/patterns -- as node -- success") {
    val response: HttpResponse[String] = Http(URL).headers(ACCEPT).headers(NODEAUTH).asString
    info("Code: " + response.code)
    info("Body: " + response.body)
    assert(response.code === HttpCode.OK.intValue)
    val responseBody: Map[String, Pattern] = JsonMethods.parse(response.body).extract[GetPatternsResponse].patterns
    assert(responseBody.size === 2)
    assert(responseBody.contains(TESTPATTERNS(0).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(0).pattern), TESTPATTERNS(0))
    assert(responseBody.contains(TESTPATTERNS(1).pattern))
    assertPatternsEqual(responseBody(TESTPATTERNS(1).pattern), TESTPATTERNS(1))
  }

}
