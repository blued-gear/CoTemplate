package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.dto.TemplateCreateDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.RestAssured.`when`
import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.http.HttpStatus
import org.hamcrest.core.StringContains
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
@QuarkusTest
internal class AuthTest() {

    private fun createTemplate(name: String): TemplateCreatedDto {
        val createResp: TemplateCreatedDto
        given()
            .body(TemplateCreateDto(name, 16, 16))
        `when`()
            .post("/templates")
            .then()
            .statusCode(HttpStatus.SC_CREATED)
            .extract().let { resp ->
                createResp = resp.body().`as`(TemplateCreatedDto::class.java)
            }

        createResp.uniqueName shouldEndWith "-$name"
        createResp.ownerUsername.isEmpty() shouldBe false
        createResp.ownerPassword.isEmpty() shouldBe false

        return createResp
    }

    @Test
    fun shouldCreateAccountOnTemplateCreate() {
        val template = createTemplate("t1")

        given()
            .formParam("username", template.ownerUsername)
            .formParam("password", template.ownerPassword)
            .formParam("template", template.uniqueName)
        .`when`()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.SC_OK)
    }

    @Test
    fun rejectsNonexistingTemplate() {
        val template = createTemplate("t1")

        given()
            .formParam("username", template.ownerUsername)
            .formParam("password", template.ownerPassword)
            .formParam("template", template.uniqueName + "_")
        .`when`()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.SC_FORBIDDEN)
            .body("todo.message", StringContains("template '${template.uniqueName + "_"}' does not exist"))
    }

    @Test
    fun rejectsNonexistingUser() {
        val template = createTemplate("t1")

        given()
            .formParam("username", template.ownerUsername + "_")
            .formParam("password", template.ownerPassword)
            .formParam("template", template.uniqueName)
        .`when`()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.SC_FORBIDDEN)
            .body("todo.message", StringContains("user '${template.ownerUsername + "_"}' does not exist for template ${template.uniqueName}"))
    }

    @Test
    fun rejectsWrongPassword() {
        val template = createTemplate("t1")

        given()
            .formParam("username", template.ownerUsername)
            .formParam("password", template.ownerPassword + "_")
            .formParam("template", template.uniqueName)
        .`when`()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.SC_FORBIDDEN)
            .body("todo.message", StringContains("invalid password"))
    }
}
