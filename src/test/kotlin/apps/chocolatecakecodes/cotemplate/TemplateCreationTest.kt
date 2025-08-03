package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreateDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.exception.ExceptionBody
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
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
import java.util.*

@QuarkusTest
internal class TemplateCreationTest {

    @BeforeEach
    @ActivateRequestContext
    @Transactional
    fun cleanDb() {
        UserEntity.deleteAll()
        TemplateEntity.deleteAll()
    }

    @Test
    fun shouldCreateTemplate() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 16, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_CREATED)
            this.extract().body().`as`(TemplateCreatedDto::class.java).let { resp ->
                resp.uniqueName shouldEndWith "-tpl1"
            }
        }
    }

    @Test
    fun shouldFailOnExistingName() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 16, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_CREATED)
        }

        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 18, 18))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_CONFLICT)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("template with name 'tpl1' already exists")
            }
        }
    }

    @Test
    fun shouldFailOnInvalidName() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1-", 16, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("invalid name")
            }
        }
    }

    @Test
    fun shouldFailOnLongName() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("t1".repeat(64) + "0", 16, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("invalid name")
            }
        }
    }

    @Test
    fun shouldFailOnShortName() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("t10", 16, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("invalid name")
            }
        }
    }

    @Test
    fun shouldFailOnNegativeWidth() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", -16, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("template dimensions must be > 0 and <= 8192")
            }
        }
    }

    @Test
    fun shouldFailOnZeroWidth() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 0, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("template dimensions must be > 0 and <= 8192")
            }
        }
    }

    @Test
    fun shouldFailOnNegativeHeight() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 16, -16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("template dimensions must be > 0 and <= 8192")
            }
        }
    }

    @Test
    fun shouldFailOnZeroHeight() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 16, 0))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("template dimensions must be > 0 and <= 8192")
            }
        }
    }

    @Test
    fun shouldFailOnBigWidth() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 8193, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("template dimensions must be > 0 and <= 8192")
            }
        }
    }

    @Test
    fun shouldFailOnBigHeight() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 16, 8193))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_BAD_REQUEST)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message.shouldBe("template dimensions must be > 0 and <= 8192")
            }
        }
    }

    @Test
    fun shouldSuccessOnMaxSize() {
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 8192, 8192))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_CREATED)
        }
    }

    @Test
    fun shouldHaveCorrectDetails() {
        var name: String? = null
        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateCreateDto("tpl1", 18, 16))
        } When {
            this.post("/templates")
        } Then {
            this.statusCode(HttpStatus.SC_CREATED)
            this.extract().body().`as`(TemplateCreatedDto::class.java).let { resp ->
                name = resp.uniqueName
            }
        }
        name!!

        When {
            this.get("/templates/$name")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateDetailsDto::class.java).let { resp ->
                resp.name.shouldBe("tpl1")
                resp.width.shouldBe(18)
                resp.height.shouldBe(16)
                resp.templateCount.shouldBe(0)

                val now = System.currentTimeMillis()
                Date(resp.createdAt).shouldBeIn(Date(now - 1500)..Date(now + 1500))
            }
        }
    }
}
