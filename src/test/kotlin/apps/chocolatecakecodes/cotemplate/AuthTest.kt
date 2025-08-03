package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreateDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.transaction.Transactional
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
internal class AuthTest {

    private fun createTemplate(name: String): TemplateCreatedDto {
        var createResp: TemplateCreatedDto? = null
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto(name, 16, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_CREATED)
            this.extract().let { resp ->
                createResp = resp.body().`as`(TemplateCreatedDto::class.java)
            }
        }
        createResp!!

        createResp.ownerUsername.isEmpty() shouldBe false
        createResp.ownerPassword.isEmpty() shouldBe false

        return createResp
    }

    @BeforeEach
    @ActivateRequestContext
    @Transactional
    fun cleanDb() {
        UserEntity.deleteAll()
        TemplateEntity.deleteAll()
    }

    @Test
    fun shouldCreateAccountOnTemplateCreate() {
        val template = createTemplate("tpl1")

        Given {
            this.formParam("username", template.ownerUsername)
            this.formParam("password", template.ownerPassword)
            this.formParam("template", template.uniqueName)
        } When {
            this.post("/auth/login")
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
            this.post("/auth/login")
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
            this.post("/auth/login")
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
            this.post("/auth/login")
        } Then {
            this.statusCode(HttpStatus.SC_UNAUTHORIZED)
            //this.body("todo.message", StringContains("invalid password"))
        }
    }
}
