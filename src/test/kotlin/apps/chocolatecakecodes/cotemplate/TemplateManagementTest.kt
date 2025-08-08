package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateUpdateSizeDto
import apps.chocolatecakecodes.cotemplate.exception.ExceptionBody
import apps.chocolatecakecodes.cotemplate.util.CleanupHelper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
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
        val tpl = TemplateItemTest.setupTemplate()

        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateSizeDto(12, 13))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/size")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateDetailsDto::class.java).let { resp ->
                resp.name shouldBe "tpl1"
                resp.width shouldBe 12
                resp.height shouldBe 13
            }
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateDetailsDto::class.java).let { resp ->
                resp.name shouldBe "tpl1"
                resp.width shouldBe 12
                resp.height shouldBe 13
            }
        }
    }

    @Test
    fun updateDimensionsFailsOnInvalidDimension() {
        val tpl = TemplateItemTest.setupTemplate()

        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateSizeDto(12, 0))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/size")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message shouldStartWith "template dimensions must be > 0 and <= "
            }
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateDetailsDto::class.java).let { resp ->
                resp.name shouldBe "tpl1"
                resp.width shouldBe 256
                resp.height shouldBe 128
            }
        }
    }
}
