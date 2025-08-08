package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.dto.TemplateDetailsDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemUpdateDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemsDto
import apps.chocolatecakecodes.cotemplate.util.*
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
internal class TemplateItemTest {

    companion object {

        const val IMG1 = "/images/templates/1.png"
        const val IMG2 = "/images/templates/2.png"
    }

    @Inject
    private lateinit var cleanupHelper: CleanupHelper

    @BeforeEach
    fun cleanDb() {
        cleanupHelper.cleanDb()
    }

    @BeforeEach
    fun cleanImages() {
        cleanupHelper.cleanImages()
    }

    @Test
    fun emptyTemplate() {
        val tpl = createTemplate()

        When {
            this.get("/api/templates/${tpl.uniqueName}/items")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemsDto::class.java)
        } Let { resp ->
            resp.items.shouldBeEmpty()
        }
    }

    @Test
    fun addItems() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val item1 = uploadItem(auth, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)
        item1.asClue {
            it.description shouldBe "i 1"
            it.id shouldNotBe 0
            it.x shouldBe 1
            it.y shouldBe 2
            it.z shouldBe 0
            it.width shouldBe 48
            it.height shouldBe 32
        }
        val item2 = uploadItem(auth, tpl.uniqueName, "i 2", 3, 4, 1, IMG2)
        item2.asClue {
            it.description shouldBe "i 2"
            it.id shouldNotBe 0
            it.x shouldBe 3
            it.y shouldBe 4
            it.z shouldBe 1
            it.width shouldBe 32
            it.height shouldBe 32
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}/items/${item1.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item1
        }
        When {
            this.get("/api/templates/${tpl.uniqueName}/items/${item2.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item2
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}/items")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemsDto::class.java)
        } Let { resp ->
            resp.items.shouldContainExactlyInAnyOrder(item1, item2)
        }
    }

    @Test
    fun addItemIncreasesCount() {
        val tpl = createTemplate()
        val auth = login(tpl)

        uploadItem(auth, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)
        uploadItem(auth, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)

        When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.templateCount shouldBe 2
        }
    }

    @Test
    fun getItemImage() {
        val tpl = createTemplate()
        val auth = login(tpl)

        uploadItem(auth, tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item2 = uploadItem(auth, tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        When {
            this.get("/api/templates/${tpl.uniqueName}/items/${item2.id}/image")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.contentType("image/png")
        } Extract {
            this.body().asByteArray()
        } Let { resp ->
            val expected = TemplateItemTest::class.java.getResourceAsStream(IMG2).use {
                it.readAllBytes()
            }
            resp shouldBe expected
        }
    }

    @Test
    fun deleteItem() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val item1 = uploadItem(auth, tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item2 = uploadItem(auth, tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.cookie(auth)
        } When {
            this.delete("/api/templates/${tpl.uniqueName}/items/${item1.id}")
        } Then {
            this.statusCode(HttpStatus.SC_NO_CONTENT)
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}/items")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemsDto::class.java)
        } Let { resp ->
            resp.items.shouldContainExactlyInAnyOrder(item2)
        }
    }

    @Test
    fun deleteItemDecreasesCount() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val item = uploadItem(auth, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)

        When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.templateCount shouldBe 1
        }

        Given {
            this.cookie(auth)
        } When {
            this.delete("/api/templates/${tpl.uniqueName}/items/${item.id}")
        } Then {
            this.statusCode(HttpStatus.SC_NO_CONTENT)
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let { resp ->
            resp.templateCount shouldBe 0
        }
    }

    @Test
    fun updateNothing() {
        val tpl = createTemplate()
        val auth = login(tpl)

        uploadItem(auth, tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item = uploadItem(auth, tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto())
        } When {
            this.put("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item
        }
    }

    @Test
    fun updateDescription() {
        val tpl = createTemplate()
        val auth = login(tpl)

        uploadItem(auth, tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item = uploadItem(auth, tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(description = "desc"))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item.copy(description = "desc")
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item.copy(description = "desc")
        }
    }

    @Test
    fun updatePosition() {
        val tpl = createTemplate()
        val auth = login(tpl)

        uploadItem(auth, tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item = uploadItem(auth, tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(x = 1000, y = -1000, z = 5))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item.copy(x = 1000, y = -1000, z = 5)
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item.copy(x = 1000, y = -1000, z = 5)
        }
    }

    @Test
    fun updateImage() {
        val tpl = createTemplate()
        val auth = login(tpl)

        uploadItem(auth, tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item = uploadItem(auth, tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.MULTIPART)
            TemplateItemTest::class.java.getResourceAsStream(IMG1).use {
                it.readAllBytes()
            }.let {
                this.multiPart("image", "image.png", it)
            }
        } When {
            this.put("/api/templates/${tpl.uniqueName}/items/${item.id}/image")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item.copy(width = 48, height = 32)
        }

        When {
            this.get("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemDto::class.java)
        } Let { resp ->
            resp shouldBe item.copy(width = 48, height = 32)
        }
        When {
            this.get("/api/templates/${tpl.uniqueName}/items/${item.id}/image")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.contentType("image/png")
        } Extract {
            this.body().asByteArray()
        } Let { resp ->
            val expected = TemplateItemTest::class.java.getResourceAsStream(IMG1).use {
                it.readAllBytes()
            }
            resp shouldBe expected
        }
    }
}
