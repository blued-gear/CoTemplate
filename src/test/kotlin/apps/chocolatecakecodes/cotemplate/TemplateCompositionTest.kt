package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG1
import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG2
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemUpdateDto
import apps.chocolatecakecodes.cotemplate.exception.ExceptionBody
import apps.chocolatecakecodes.cotemplate.util.CleanupHelper
import apps.chocolatecakecodes.cotemplate.util.createTemplate
import apps.chocolatecakecodes.cotemplate.util.login
import apps.chocolatecakecodes.cotemplate.util.uploadItem
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.inject.Inject
import org.apache.http.HttpStatus
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random
import kotlin.random.nextInt
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

    @Inject
    private lateinit var cleanupHelper: CleanupHelper

    private fun checkImg(tpl: String, imgs: List<String>, expectedClasspath: String) {
        checkImg("/api/templates/$tpl/template?images=${imgs.joinToString(",")}", expectedClasspath)
    }

    private fun checkImg(urlPath: String, expectedClasspath: String) {
        val actual = When {
            this.get(urlPath)
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.contentType("image/png")
        } Extract {
            this.body().asByteArray().let {
                ImmutableImage.loader().fromBytes(it)
            }
        }

        val expected = TemplateCompositionTest::class.java.getResourceAsStream(expectedClasspath).use {
            ImmutableImage.loader().fromStream(it)
        }

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
        checkImg(tpl.uniqueName, emptyList(), EXPECTED_EMPTY)
    }

    @Test
    fun bounds() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val i1 = uploadItem(auth, tpl.uniqueName, "", 0, 0, 0, IMG1)
        val i2 = uploadItem(auth, tpl.uniqueName, "", 256 - 32, 128 - 32, 0, IMG2)
        checkImg(tpl.uniqueName, listOf(i1.id, i2.id), EXPECTED_BOUNDS)
    }

    @Test
    fun overlay() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val i1 = uploadItem(auth, tpl.uniqueName, "", 33, 0, 1, IMG2)
        val i2 = uploadItem(auth, tpl.uniqueName, "", 48, 15, 0, IMG1)
        checkImg(tpl.uniqueName, listOf(i1.id, i2.id), EXPECTED_OVERLAY)
    }

    @Test
    fun oneItem() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val i1 = uploadItem(auth, tpl.uniqueName, "", 0, 0, 0, IMG1)
        uploadItem(auth, tpl.uniqueName, "", 256 - 32, 128 - 32, 0, IMG2)
        checkImg(tpl.uniqueName, listOf(i1.id), EXPECTED_ONE)
    }

    @Test
    fun partialOutside() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val i1 = uploadItem(auth, tpl.uniqueName, "", -4, -3, 0, IMG1)
        val i2 = uploadItem(auth, tpl.uniqueName, "", 229, 102, 0, IMG2)
        checkImg(tpl.uniqueName, listOf(i1.id, i2.id), EXPECTED_PARTIAL_OUTSIDE)
    }

    @Test
    fun outside() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val i1 = uploadItem(auth, tpl.uniqueName, "", -100, -100, 0, IMG1)
        checkImg(tpl.uniqueName, listOf(i1.id), EXPECTED_EMPTY)
    }

    @Test
    fun all() {
        val tpl = createTemplate()
        val auth = login(tpl)

        uploadItem(auth, tpl.uniqueName, "", 0, 0, 0, IMG1)
        uploadItem(auth, tpl.uniqueName, "", 256 - 32, 128 - 32, 0, IMG2)
        checkImg("/api/templates/${tpl.uniqueName}/template?images=all", EXPECTED_BOUNDS)
    }

    @Test
    fun itemNotFound() {
        val tpl = createTemplate()
        val auth = login(tpl)
        val i1 = uploadItem(auth, tpl.uniqueName, "", -100, -100, 0, IMG1)

        When {
            this.get("/api/templates/${tpl.uniqueName}/template?images=${i1.id},123")
        } Then {
            this.statusCode(HttpStatus.SC_NOT_FOUND)
            this.contentType(ContentType.JSON)
        } Extract {
            this.body().`as`(ExceptionBody::class.java).let { resp ->
                resp.message shouldBe "at least one item of {${i1.id}, 123} does not exist in template '${tpl.uniqueName}'"
            }
        }
    }

    @Test
    fun caching() {
        val tpl = createTemplate()
        val auth = login(tpl)
        val i1 = uploadItem(auth, tpl.uniqueName, "", 5, 5, 0, IMG1)
        val i2 = uploadItem(auth, tpl.uniqueName, "", 10, 10, 0, IMG1)
        val i3 = uploadItem(auth, tpl.uniqueName, "", 15, 15, 0, IMG1)
        val i4 = uploadItem(auth, tpl.uniqueName, "", 20, 20, 0, IMG1)
        val i5 = uploadItem(auth, tpl.uniqueName, "", 25, 25, 0, IMG1)

        val ids = listOf(i1.id, i2.id, i3.id, i4.id, i5.id).joinToString(",")
        val tNew = measureTime {
            When {
                this.get("/api/templates/${tpl.uniqueName}/template?images=$ids")
            } Then {
                this.statusCode(HttpStatus.SC_OK)
            }
        }
        val tCached = measureTime {
            When {
                this.get("/api/templates/${tpl.uniqueName}/template?images=$ids")
            } Then {
                this.statusCode(HttpStatus.SC_OK)
            }
        }

        withClue(Pair(tNew, tCached)) {
            println("speedup: ${tNew.minus(tCached)}")
            tCached shouldBeLessThan tNew.times(0.7)
        }
    }

    @Test
    fun cacheInvalidationOnItemUpdate() {
        val tpl = createTemplate()
        val auth = login(tpl)

        val i1 = uploadItem(auth, tpl.uniqueName, "", 0, 0, 0, IMG1)
        checkImg(tpl.uniqueName, listOf(i1.id), EXPECTED_ONE)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(x = 1000, y = -1000, z = 5))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/items/${i1.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        }

        checkImg(tpl.uniqueName, listOf(i1.id), EXPECTED_EMPTY)
    }
}
