package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.exception.ExceptionBody
import apps.chocolatecakecodes.cotemplate.util.CleanupHelper
import apps.chocolatecakecodes.cotemplate.util.Let
import apps.chocolatecakecodes.cotemplate.util.createTemplate
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.inject.Inject
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

@QuarkusTest
internal class TemplateCreationTest {

    @Inject
    private lateinit var cleanupHelper: CleanupHelper

    @BeforeEach
    fun cleanDb() {
        cleanupHelper.cleanDb()
    }

    @Test
    fun shouldCreateTemplate() {
        createTemplate("tpl1", 16, 16, TeamCreatePolicy.EVERYONE).let {
            it.uniqueName shouldEndWith "-tpl1"
        }
    }

    @Test
    fun shouldFailOnExistingName() {
        createTemplate("tpl1")

        createTemplate("tpl1", 18, 18) {
            Then {
                this.statusCode(HttpStatus.SC_CONFLICT)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "template with name 'tpl1' already exists"
            }
        }
    }

    @Test
    fun shouldFailOnInvalidName() {
        createTemplate("tpl1-") {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "invalid name"
            }
        }
    }

    @Test
    fun shouldFailOnLongName() {
        createTemplate("t1".repeat(64) + "0") {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "invalid name"
            }
        }
    }

    @Test
    fun shouldFailOnShortName() {
        createTemplate("t10") {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "invalid name"
            }
        }
    }

    @Test
    fun shouldFailOnNegativeWidth() {
        createTemplate(w = -16, h = 16) {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "template dimensions must be > 0 and <= 8192"
            }
        }
    }

    @Test
    fun shouldFailOnZeroWidth() {
        createTemplate(w = 0, h = 16) {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "template dimensions must be > 0 and <= 8192"
            }
        }
    }

    @Test
    fun shouldFailOnNegativeHeight() {
        createTemplate(w = 16, h = -16) {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "template dimensions must be > 0 and <= 8192"
            }
        }
    }

    @Test
    fun shouldFailOnZeroHeight() {
        createTemplate(w = 16, h = -16) {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "template dimensions must be > 0 and <= 8192"
            }
        }
    }

    @Test
    fun shouldFailOnBigWidth() {
        createTemplate(w = 8193, h = 16) {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "template dimensions must be > 0 and <= 8192"
            }
        }
    }

    @Test
    fun shouldFailOnBigHeight() {
        createTemplate(w = 16, h = 8193) {
            Then {
                this.statusCode(HttpStatus.SC_BAD_REQUEST)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "template dimensions must be > 0 and <= 8192"
            }
        }
    }

    @Test
    fun shouldSuccessOnMaxSize() {
        createTemplate(w = 8192, h = 8192)
    }

    @Test
    fun shouldHaveCorrectDetails() {
        val tpl = createTemplate("tpl1", 18, 16, TeamCreatePolicy.EVERYONE)

        val resp = When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        }

        resp.name shouldBe "tpl1"
        resp.width shouldBe 18
        resp.height shouldBe 16
        resp.teamCreatePolicy shouldBe TeamCreatePolicy.EVERYONE
        resp.templateCount shouldBe 0

        val now = System.currentTimeMillis()
        Date(resp.createdAt).shouldBeIn(Date(now - 1500)..Date(now + 1500))
    }
}
