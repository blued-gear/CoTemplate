package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG1
import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG2
import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.transaction.Transactional
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
internal class TemplateCompositionTest {

    companion object {

        const val EXPECTED_EMPTY = "/images/expected/empty.png"
        const val EXPECTED_BOUNDS = "/images/expected/bounds.png"
        const val EXPECTED_OVERLAY = "/images/expected/overlay.png"
    }

    private fun checkImg(tpl: String, expectedClasspath: String) {
        When {
            this.get("/templates/$tpl/template")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.contentType("image/png")
            val actual = this.extract().body().asByteArray()

            TemplateCompositionTest::class.java.getResourceAsStream(expectedClasspath).use {
                it.readAllBytes()
            }.let { expected ->
                actual shouldBe expected
            }
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
        val tpl = TemplateItemTest.setupTemplate()
        checkImg(tpl.uniqueName, EXPECTED_EMPTY)
    }

    @Test
    fun bounds() {
        val tpl = TemplateItemTest.setupTemplate()
        TemplateItemTest.uploadItem(tpl.uniqueName, "", 0, 0, 0, IMG1)
        TemplateItemTest.uploadItem(tpl.uniqueName, "", 256 - 32, 128 - 32, 0, IMG2)
        checkImg(tpl.uniqueName, EXPECTED_BOUNDS)
    }

    @Test
    fun overlay() {
        val tpl = TemplateItemTest.setupTemplate()
        TemplateItemTest.uploadItem(tpl.uniqueName, "", 33, 0, 1, IMG2)
        TemplateItemTest.uploadItem(tpl.uniqueName, "", 48, 15, 0, IMG1)
        checkImg(tpl.uniqueName, EXPECTED_OVERLAY)
    }
}
