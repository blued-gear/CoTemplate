package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.*
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
internal class TemplateItemTest {

    companion object {

        const val IMG1 = "/images/templates/1.png"
        const val IMG2 = "/images/templates/2.png"

        fun setupTemplate(): TemplateCreatedDto {
            var ret: TemplateCreatedDto? = null
            Given {
                this.contentType(ContentType.JSON)
                this.body(TemplateCreateDto("tpl1", 256, 128))
            } When {
                this.post("/templates")
            } Then {
                this.statusCode(HttpStatus.SC_CREATED)
                this.extract().let { resp ->
                    ret = resp.body().`as`(TemplateCreatedDto::class.java)
                }
            }
            return ret!!
        }

        fun uploadItem(tpl: String, desc: String, x: Int, y: Int, z: Int, classpath: String): TemplateItemDto {
            var ret: TemplateItemDto? = null
            Given {
                this.contentType(ContentType.MULTIPART)
                this.multiPart("description", desc)
                this.multiPart("x", x.toString())
                this.multiPart("y", y.toString())
                this.multiPart("z", z.toString())

                TemplateItemTest::class.java.getResourceAsStream(classpath).use {
                    it.readAllBytes()
                }.let {
                    this.multiPart("image", "image.png", it)
                }
            } When {
                this.post("/templates/$tpl/items")
            } Then {
                this.statusCode(HttpStatus.SC_CREATED)
                ret = this.extract().body().`as`(TemplateItemDto::class.java)
            }
            return ret!!
        }
    }

    @BeforeEach
    @ActivateRequestContext
    @Transactional
    fun cleanDb() {
        ;//TODO clear item repo
        UserEntity.deleteAll()
        TemplateEntity.deleteAll()
    }

    @Test
    fun emptyTemplate() {
        val tpl = setupTemplate()
        When {
            this.get("/templates/${tpl.uniqueName}/items")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemsDto::class.java).let { resp ->
                resp.items.shouldBeEmpty()
            }
        }
    }

    @Test
    fun addItems() {
        val tpl = setupTemplate()

        val item1 = uploadItem(tpl.uniqueName, "i 1", 1, 2, 0, IMG1)
        item1.asClue {
            it.description shouldBe "i 1"
            it.id shouldNotBe 0
            it.x shouldBe 1
            it.y shouldBe 2
            it.z shouldBe 0
            it.width shouldBe 48
            it.height shouldBe 32
        }
        val item2 = uploadItem(tpl.uniqueName, "i 2", 3, 4, 1, IMG2)
        item1.asClue {
            it.description shouldBe "i 2"
            it.id shouldNotBe 0
            it.x shouldBe 3
            it.y shouldBe 4
            it.z shouldBe 1
            it.width shouldBe 32
            it.height shouldBe 32
        }

        When {
            this.get("/templates/${tpl.uniqueName}/items/${item1.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemDto::class.java).let { resp ->
                resp shouldBe item1
            }
        }
        When {
            this.get("/templates/${tpl.uniqueName}/items/${item2.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemDto::class.java).let { resp ->
                resp shouldBe item2
            }
        }

        When {
            this.get("/templates/${tpl.uniqueName}/items")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemsDto::class.java).let { resp ->
                resp.items.shouldContainExactlyInAnyOrder(item1, item2)
            }
        }
    }

    @Test
    fun getItemImage() {
        val tpl = setupTemplate()
        uploadItem(tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item2 = uploadItem(tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        When {
            this.get("/templates/${tpl.uniqueName}/items/${item2.id}/image")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.contentType("image/png")
            this.extract().body().asByteArray().let { resp ->
                val expected = TemplateItemTest::class.java.getResourceAsStream(IMG2).use {
                    it.readAllBytes()
                }
                resp shouldBe expected
            }
        }
    }

    @Test
    fun deleteItem() {
        val tpl = setupTemplate()
        val item1 = uploadItem(tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item2 = uploadItem(tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        When {
            this.delete("/templates/${tpl.uniqueName}/items/${item1.id}")
        } Then {
            this.statusCode(HttpStatus.SC_NO_CONTENT)
        }

        When {
            this.get("/templates/${tpl.uniqueName}/items")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemsDto::class.java).let { resp ->
                resp.items.shouldContainExactlyInAnyOrder(item2)
            }
        }
    }

    @Test
    fun updateNothing() {
        val tpl = setupTemplate()
        uploadItem(tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item = uploadItem(tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto())
        } When {
            this.put("/templates/${tpl.uniqueName}/items/${item.id}/details")
        }
        When {
            this.get("/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemDto::class.java).let { resp ->
                resp shouldBe item
            }
        }
    }

    @Test
    fun updateDescription() {
        val tpl = setupTemplate()
        uploadItem(tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item = uploadItem(tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(description = "desc"))
        } When {
            this.put("/templates/${tpl.uniqueName}/items/${item.id}/details")
        }
        When {
            this.get("/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemDto::class.java).let { resp ->
                resp shouldBe item.copy(description = "desc")
            }
        }
    }

    @Test
    fun updatePosition() {
        val tpl = setupTemplate()
        uploadItem(tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item = uploadItem(tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(x = 1000, y = -1000, z = 5))
        } When {
            this.put("/templates/${tpl.uniqueName}/items/${item.id}/details")
        }
        When {
            this.get("/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemDto::class.java).let { resp ->
                resp shouldBe item.copy(x = 1000, y = -1000, z = 5)
            }
        }
    }

    @Test
    fun updateImage() {
        val tpl = setupTemplate()
        uploadItem(tpl.uniqueName, "i 1", 0, 0, 0, IMG1)
        val item = uploadItem(tpl.uniqueName, "i 2", 0, 0, 0, IMG2)

        Given {
            this.contentType(ContentType.MULTIPART)
            TemplateItemTest::class.java.getResourceAsStream(IMG1).use {
                it.readAllBytes()
            }.let {
                this.multiPart("image", "image.png", it)
            }
        } When {
            this.put("/templates/${tpl.uniqueName}/items/${item.id}/image")
        } Then {
            this.statusCode(HttpStatus.SC_NO_CONTENT)
        }

        When {
            this.get("/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.extract().body().`as`(TemplateItemDto::class.java).let { resp ->
                resp shouldBe item.copy(width = 48, height = 32)
            }
        }
        When {
            this.get("/templates/${tpl.uniqueName}/items/${item.id}/image")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.contentType("image/png")
            this.extract().body().asByteArray().let { resp ->
                val expected = TemplateItemTest::class.java.getResourceAsStream(IMG1).use {
                    it.readAllBytes()
                }
                resp shouldBe expected
            }
        }
    }
}
