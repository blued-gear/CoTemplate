package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG1
import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG2
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.TemplateItemEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemUpdateDto
import apps.chocolatecakecodes.cotemplate.exception.ExceptionBody
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.apache.http.HttpStatus
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

@QuarkusTest
internal class TemplateCompositionTest {

    companion object {

        const val EXPECTED_EMPTY = "/images/expected/empty.png"
        const val EXPECTED_BOUNDS = "/images/expected/bounds.png"
        const val EXPECTED_OVERLAY = "/images/expected/overlay.png"
        const val EXPECTED_ONE = "/images/expected/one.png"
        const val EXPECTED_PARTIAL_OUTSIDE = "/images/expected/partialOutside.png"
    }

    @Inject
    @ConfigProperty(name = "cotemplate.image-storage")
    lateinit var imgDirPath: String

    private fun checkImg(tpl: String, imgs: List<String>, expectedClasspath: String) {
        checkImg("/templates/$tpl/template?images=${imgs.joinToString(",")}", expectedClasspath)
    }

    private fun checkImg(urlPath: String, expectedClasspath: String) {
        When {
            this.get(urlPath)
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.contentType("image/png")
            val actual = this.extract().body().asByteArray().let {
                ImmutableImage.loader().fromBytes(it)
            }

            TemplateCompositionTest::class.java.getResourceAsStream(expectedClasspath).use {
                ImmutableImage.loader().fromStream(it)
            }.let { expected ->
                try {
                    actual.width shouldBe expected.width
                    actual.height shouldBe expected.height
                    actual.hasAlpha() shouldBe true

                    for(y in (0..<actual.height)) {
                        for(x in (0..<actual.width)) {
                            val actualPx = actual.pixel(x, y)
                            val expectedPx = expected.pixel(x, y)

                            withClue(Pair(x, y)) {
                                actualPx.alpha() shouldBe expectedPx.alpha()
                                if(actualPx.alpha() != 0)
                                    actualPx shouldBe expectedPx
                            }
                        }
                    }
                } catch(e: AssertionError) {
                    val name = Random.nextInt(1..1000).toString() + ".png"
                    val path = Path.of(imgDirPath, "testfail", name)
                    Files.createDirectories(path.parent)
                    actual.output(PngWriter.MaxCompression, path)
                    println("failed image written to $name")

                    throw e
                }
            }
        }
    }

    @BeforeEach
    @ActivateRequestContext
    @Transactional
    fun cleanDb() {
        TemplateItemEntity.deleteAll()
        UserEntity.deleteAll()
        TemplateEntity.deleteAll()
    }

    @BeforeEach
    @ActivateRequestContext
    @Transactional
    fun cleanImages() {
        Files.list(Path.of(imgDirPath)).forEach {
            it.toFile().deleteRecursively()
        }
    }

    @Test
    fun emptyTemplate() {
        val tpl = TemplateItemTest.setupTemplate()
        checkImg(tpl.uniqueName, emptyList(), EXPECTED_EMPTY)
    }

    @Test
    fun bounds() {
        val tpl = TemplateItemTest.setupTemplate()
        val i1 = TemplateItemTest.uploadItem(tpl.uniqueName, "", 0, 0, 0, IMG1)
        val i2 = TemplateItemTest.uploadItem(tpl.uniqueName, "", 256 - 32, 128 - 32, 0, IMG2)
        checkImg(tpl.uniqueName, listOf(i1.id, i2.id), EXPECTED_BOUNDS)
    }

    @Test
    fun overlay() {
        val tpl = TemplateItemTest.setupTemplate()
        val i1 = TemplateItemTest.uploadItem(tpl.uniqueName, "", 33, 0, 1, IMG2)
        val i2 = TemplateItemTest.uploadItem(tpl.uniqueName, "", 48, 15, 0, IMG1)
        checkImg(tpl.uniqueName, listOf(i1.id, i2.id), EXPECTED_OVERLAY)
    }

    @Test
    fun oneItem() {
        val tpl = TemplateItemTest.setupTemplate()
        val i1 = TemplateItemTest.uploadItem(tpl.uniqueName, "", 0, 0, 0, IMG1)
        TemplateItemTest.uploadItem(tpl.uniqueName, "", 256 - 32, 128 - 32, 0, IMG2)
        checkImg(tpl.uniqueName, listOf(i1.id), EXPECTED_ONE)
    }

    @Test
    fun partialOutside() {
        val tpl = TemplateItemTest.setupTemplate()
        val i1 = TemplateItemTest.uploadItem(tpl.uniqueName, "", -4, -3, 0, IMG1)
        val i2 = TemplateItemTest.uploadItem(tpl.uniqueName, "", 229, 102, 0, IMG2)
        checkImg(tpl.uniqueName, listOf(i1.id, i2.id), EXPECTED_PARTIAL_OUTSIDE)
    }

    @Test
    fun outside() {
        val tpl = TemplateItemTest.setupTemplate()
        val i1 = TemplateItemTest.uploadItem(tpl.uniqueName, "", -100, -100, 0, IMG1)
        checkImg(tpl.uniqueName, listOf(i1.id), EXPECTED_EMPTY)
    }

    @Test
    fun all() {
        val tpl = TemplateItemTest.setupTemplate()
        TemplateItemTest.uploadItem(tpl.uniqueName, "", 0, 0, 0, IMG1)
        TemplateItemTest.uploadItem(tpl.uniqueName, "", 256 - 32, 128 - 32, 0, IMG2)
        checkImg("/templates/${tpl.uniqueName}/template?images=all", EXPECTED_BOUNDS)
    }

    @Test
    fun itemNotFound() {
        val tpl = TemplateItemTest.setupTemplate()
        val i1 = TemplateItemTest.uploadItem(tpl.uniqueName, "", -100, -100, 0, IMG1)

        When {
            this.get("/templates/${tpl.uniqueName}/template?images=${i1.id},123")
        } Then {
            this.statusCode(HttpStatus.SC_NOT_FOUND)
            this.contentType(ContentType.JSON)
            this.extract().body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message shouldBe "at least one item of {${i1.id}, 123} does not exist in template '${tpl.uniqueName}'"
            }
        }
    }

    @Test
    fun caching() {
        val tpl = TemplateItemTest.setupTemplate()
        val i1 = TemplateItemTest.uploadItem(tpl.uniqueName, "", -100, -100, 0, IMG1)

        val tNew = measureTime {
            When {
                this.get("/templates/${tpl.uniqueName}/template?images=${i1.id}")
            } Then {
                this.statusCode(HttpStatus.SC_OK)
            }
        }
        val tCached = measureTime {
            When {
                this.get("/templates/${tpl.uniqueName}/template?images=${i1.id}")
            } Then {
                this.statusCode(HttpStatus.SC_OK)
            }
        }

        withClue(Pair(tNew, tCached)) {
            println("speedup: ${tNew.minus(tCached)}")
            tNew.minus(tCached) shouldBeGreaterThan 1.milliseconds
        }
    }

    @Test
    fun cacheInvalidationOnItemUpdate() {
        val tpl = TemplateItemTest.setupTemplate()
        val i1 = TemplateItemTest.uploadItem(tpl.uniqueName, "", 0, 0, 0, IMG1)
        checkImg(tpl.uniqueName, listOf(i1.id), EXPECTED_ONE)

        Given {
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(x = 1000, y = -1000, z = 5))
        } When {
            this.put("/templates/${tpl.uniqueName}/items/${i1.id}/details")
        }

        checkImg(tpl.uniqueName, listOf(i1.id), EXPECTED_EMPTY)
    }
}
