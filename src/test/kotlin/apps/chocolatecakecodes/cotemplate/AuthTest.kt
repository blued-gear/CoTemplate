package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.dto.UserInfo
import apps.chocolatecakecodes.cotemplate.dto.UserInfoDto
import apps.chocolatecakecodes.cotemplate.util.*
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.inject.Inject
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
internal class AuthTest {

    @Inject
    private lateinit var cleanupHelper: CleanupHelper

    @BeforeEach
    fun cleanDb() {
        cleanupHelper.cleanDb()
    }

    @Test
    fun shouldCreateAccountOnTemplateCreate() {
        val template = createTemplate("tpl1")

        Given {
            this.formParam("username", template.ownerUsername)
            this.formParam("password", template.ownerPassword)
            this.formParam("template", template.uniqueName)
        } When {
            this.post("/api/auth/login")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        }
    }

    @Test
    fun rejectsNonexistingTemplate() {
        val template = createTemplate("tpl1")

        Given {
            this.formParam("username", template.ownerUsername)
            this.formParam("password", template.ownerPassword)
            this.formParam("template", template.uniqueName + "_")
        } When {
            this.post("/api/auth/login")
        } Then {
            this.statusCode(HttpStatus.SC_UNAUTHORIZED)
            //TODO find out how to respond with a custom message
            //this.body("todo.message", StringContains("template '${template.uniqueName + "_"}' does not exist"))
        }
    }

    @Test
    fun rejectsNonexistingUser() {
        val template = createTemplate("tpl1")

        Given {
            this.formParam("username", template.ownerUsername + "_")
            this.formParam("password", template.ownerPassword)
            this.formParam("template", template.uniqueName)
        } When {
            this.post("/api/auth/login")
        } Then {
            this.statusCode(HttpStatus.SC_UNAUTHORIZED)
            //this.body("todo.message", StringContains("user '${template.ownerUsername + "_"}' does not exist for template ${template.uniqueName}"))
        }
    }

    @Test
    fun rejectsWrongPassword() {
        val template = createTemplate("tpl1")

        Given {
            this.formParam("username", template.ownerUsername)
            this.formParam("password", template.ownerPassword + "_")
            this.formParam("template", template.uniqueName)
        } When {
            this.post("/api/auth/login")
        } Then {
            this.statusCode(HttpStatus.SC_UNAUTHORIZED)
            //this.body("todo.message", StringContains("invalid password"))
        }
    }

    @Test
    fun idForGuest() {
        When {
            this.get("/api/auth/id")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(UserInfoDto::class.java)
        } Let { resp ->
            resp.isGuest shouldBe true
            resp.info shouldBe null
        }
    }

    @Test
    fun idForOwner() {
        val tpl = createTemplate("tpl1")
        val auth = login(tpl)

        Given {
            this.cookie(auth)
        } When {
            this.get("/api/auth/id")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(UserInfoDto::class.java)
        } Let { resp ->
            resp.isGuest shouldBe false
            resp.info shouldBe UserInfo(
                tpl.uniqueName,
                "owner",
                "TEMPLATE_OWNER",
            )
        }
    }

    @Test
    fun idForTeam() {
        val tpl = createTemplate("tpl1")
        val team = createTeam(tpl.uniqueName, "a")
        val auth = login(team)

        Given {
            this.cookie(auth)
        } When {
            this.get("/api/auth/id")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(UserInfoDto::class.java)
        } Let { resp ->
            resp.isGuest shouldBe false
            resp.info shouldBe UserInfo(
                tpl.uniqueName,
                "a",
                "TEMPLATE_TEAM",
            )
        }
    }
}
