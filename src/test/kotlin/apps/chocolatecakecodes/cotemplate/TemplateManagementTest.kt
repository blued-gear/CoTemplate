package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateUpdateSizeDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateUpdateTeamCreatePolicyDto
import apps.chocolatecakecodes.cotemplate.exception.ExceptionBody
import apps.chocolatecakecodes.cotemplate.util.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.inject.Inject
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
internal class TemplateManagementTest {

    @Inject
    private lateinit var cleanupHelper: CleanupHelper

    @BeforeEach
    fun cleanDb() {
        cleanupHelper.cleanDb()
    }

    @Test
    fun updateDimensions() {
        val tpl = createTemplate()
        val auth = login(tpl)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateSizeDto(12, 13))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/size")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.name shouldBe "tpl1"
            resp.width shouldBe 12
            resp.height shouldBe 13
        }
    }

    @Test
    fun updateDimensionsFailsOnInvalidDimension() {
        val tpl = createTemplate()
        val auth = login(tpl)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateSizeDto(12, 0))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/size")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
        } Extract {
            this.body().`as`(ExceptionBody::class.java)
        } Let { resp ->
            resp.message shouldStartWith "template dimensions must be > 0 and <= "
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.name shouldBe "tpl1"
            resp.width shouldBe 256
            resp.height shouldBe 128
        }
    }

    @Test
    fun updateTeamCreatePolicy() {
        val tpl = createTemplate()
        val auth = login(tpl)

        When {
            this.get("api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.teamCreatePolicy shouldBe TeamCreatePolicy.EVERYONE
        }

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateTeamCreatePolicyDto(TeamCreatePolicy.OWNER))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/teamCreatePolicy")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.teamCreatePolicy shouldBe TeamCreatePolicy.OWNER
        }

        When {
            this.get("api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.teamCreatePolicy shouldBe TeamCreatePolicy.OWNER
        }
    }

    @Test
    fun updateTeamCreatePolicyFailsWithoutAuth() {
        val tpl = createTemplate()

        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateTeamCreatePolicyDto(TeamCreatePolicy.OWNER))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/teamCreatePolicy")
        } Then {
            this.statusCode(HttpStatus.SC_UNAUTHORIZED)
        }

        When {
            this.get("api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.teamCreatePolicy shouldBe TeamCreatePolicy.EVERYONE
        }
    }

    @Test
    fun updateTeamCreatePolicyFailsWithoutOwnerAuth() {
        val tpl = createTemplate()
        val ownerAuth = login(tpl)
        val team = createTeam(tpl.uniqueName, "a", ownerAuth)
        val teamAuth = login(team)

        Given {
            this.cookie(teamAuth)
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateTeamCreatePolicyDto(TeamCreatePolicy.OWNER))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/teamCreatePolicy")
        } Then {
            this.statusCode(HttpStatus.SC_FORBIDDEN)
        } Extract {
            this.body().`as`(ExceptionBody::class.java)
        } Let { resp ->
            resp.message shouldBe "modifying template settings is not permitted for you"
        }

        When {
            this.get("api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.teamCreatePolicy shouldBe TeamCreatePolicy.EVERYONE
        }
    }

    @Test
    fun updateTeamCreatePolicyFailsWithWrongOwnerAuth() {
        val tpl1 = createTemplate()
        val tpl2 = createTemplate("tpl2")
        val auth = login(tpl2)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateTeamCreatePolicyDto(TeamCreatePolicy.OWNER))
        } When {
            this.put("/api/templates/${tpl1.uniqueName}/teamCreatePolicy")
        } Then {
            this.statusCode(HttpStatus.SC_FORBIDDEN)
        } Extract {
            this.body().`as`(ExceptionBody::class.java)
        } Let { resp ->
            resp.message shouldBe "modifying template settings is not permitted for you"
        }

        When {
            this.get("api/templates/${tpl1.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.teamCreatePolicy shouldBe TeamCreatePolicy.EVERYONE
        }
    }
}
