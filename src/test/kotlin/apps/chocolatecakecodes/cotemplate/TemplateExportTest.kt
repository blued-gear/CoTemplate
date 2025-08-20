package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG1
import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG2
import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import apps.chocolatecakecodes.cotemplate.dto.*
import apps.chocolatecakecodes.cotemplate.util.*
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.inject.Inject
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
internal class TemplateExportTest {

    @Inject
    private lateinit var cleanupHelper: CleanupHelper

    @BeforeEach
    fun cleanDb() {
        cleanupHelper.cleanDb()
    }

    @Test
    fun exportAndImport() {
        // prepare original
        val tpl = createTemplate()
        val auth = login(tpl)

        val item1 = uploadItem(auth, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)
        val item2 = uploadItem(auth, tpl.uniqueName, "i 2", 10, 12, 4, IMG1)
        val item3 = uploadItem(auth, tpl.uniqueName, "i 3", 1, 20, 0, IMG2)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateSizeDto(64, 65))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/size")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        }
        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateUpdateTeamCreatePolicyDto(TeamCreatePolicy.EVERYONE))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/teamCreatePolicy")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        }
        val tplDetails = When {
            this.get("/api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        }

        val team1 = createTeam(tpl.uniqueName, "t_1")
        val team2 = createTeam(tpl.uniqueName, "t_2")

        // export and import
        val exported = Given {
            this.cookie(auth)
        } When {
            this.get("/api/templates/${tpl.uniqueName}/export")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
            this.header(HttpHeaders.CONTENT_TYPE, "application/zip")
        } Extract {
            this.body().asByteArray()
        }
        exported.size shouldBeGreaterThan 1

        val newTpl = Given {
            this.contentType("application/zip")
            this.body(exported)
        } When {
            this.post("/api/templates/clone/import")
        } Then {
            this.statusCode(HttpStatus.SC_CREATED)
        } Extract {
            this.body().`as`(TemplateCreatedDto::class.java)
        }
        newTpl.uniqueName shouldEndWith "-clone"

        // verify
        newTpl.ownerUsername shouldBe tpl.ownerUsername
        newTpl.ownerPassword shouldNotBe tpl.ownerPassword
        login(newTpl)
        login(team1.copy(template = newTpl.uniqueName))
        login(team2.copy(template = newTpl.uniqueName))

        When {
            this.get("/api/templates/${newTpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateDetailsDto::class.java)
        } Let {
            it.name shouldBe "clone"
            it.width shouldBe tplDetails.width
            it.height shouldBe tplDetails.height
            it.teamCreatePolicy shouldBe tplDetails.teamCreatePolicy
            it.templateCount shouldBe tplDetails.templateCount
            it.createdAt shouldBeGreaterThan tplDetails.createdAt
        }

        val images = When {
            this.get("/api/templates/${newTpl.uniqueName}/items")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        } Extract {
            this.body().`as`(TemplateItemsDto::class.java).items
        }
        images.size shouldBe 3
        fun cmpImg(item: TemplateItemDto, img: String) {
            val item2 = images.find { it.description == item.description }
            item2 shouldNotBe null
            item2!!
            item2.width shouldBe item.width
            item2.height shouldBe item.height
            item2.x shouldBe item.x
            item2.y shouldBe item.y
            item2.z shouldBe item.z
            item2.team shouldBe item.team
            item2.id shouldNotBe item.id

            When {
                this.get("/api/templates/${newTpl.uniqueName}/items/${item2.id}/image")
            } Then {
                this.statusCode(HttpStatus.SC_OK)
            } Extract {
                this.body().asByteArray()
            } Let { actual ->
                val expected = TemplateItemTest::class.java.getResourceAsStream(img).use {
                    it.readAllBytes()
                }
                actual shouldBe expected
            }
        }
        cmpImg(item1, IMG1)
        cmpImg(item2, IMG1)
        cmpImg(item3, IMG2)
    }

    @Test
    fun onlyOwnerCanExport() {
        val tpl = createTemplate()
        val team1 = createTeam(tpl.uniqueName, "t_1")

        When {
            this.get("/api/templates/${tpl.uniqueName}/export")
        } Then {
            this.statusCode(HttpStatus.SC_UNAUTHORIZED)
        }
        Given {
            this.cookie(login(team1))
        } When {
            this.get("/api/templates/${tpl.uniqueName}/export")
        } Then {
            this.statusCode(HttpStatus.SC_FORBIDDEN)
        }
    }
}
