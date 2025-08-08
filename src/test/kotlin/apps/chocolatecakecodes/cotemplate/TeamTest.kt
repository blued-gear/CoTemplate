package apps.chocolatecakecodes.cotemplate

import apps.chocolatecakecodes.cotemplate.TemplateItemTest.Companion.IMG1
import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemUpdateDto
import apps.chocolatecakecodes.cotemplate.exception.ExceptionBody
import apps.chocolatecakecodes.cotemplate.util.*
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
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
internal class TeamTest {

    @Inject
    private lateinit var cleanupHelper: CleanupHelper

    @BeforeEach
    fun cleanDb() {
        cleanupHelper.cleanDb()
    }

    @Test
    fun createForEveryone() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.EVERYONE)
        val team = createTeam(tpl.uniqueName, "a")

        team.name shouldEndWith "a"
        team.template shouldBe tpl.uniqueName
        team.password shouldNotBeEqual ""
    }

    @Test
    fun createForEveryoneForbidden() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.OWNER)

        createTeam(tpl.uniqueName, "a") {
            Then {
                this.statusCode(HttpStatus.SC_FORBIDDEN)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "creating teams is not permitted for you"
            }
        }
    }

    @Test
    fun createForOwner() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.OWNER)
        val auth = login(tpl)
        val team = createTeam(tpl.uniqueName, "a", auth)

        team.name shouldEndWith "a"
        team.template shouldBe tpl.uniqueName
        team.password shouldNotBeEqual ""
    }

    @Test
    fun createForbiddenForWrongOwner() {
        val tpl1 = createTemplate("tpl1", policy = TeamCreatePolicy.OWNER)
        val tpl2 = createTemplate("tpl2", policy = TeamCreatePolicy.OWNER)
        val auth = login(tpl2)

        createTeam(tpl1.uniqueName, "a", auth) {
            Then {
                this.statusCode(HttpStatus.SC_FORBIDDEN)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "creating teams is not permitted for you"
            }
        }
    }

    @Test
    fun login () {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.EVERYONE)
        val team = createTeam(tpl.uniqueName, "a")

        val auth = login(team)

        Given {
            this.cookie(auth)
        } When {
            this.get("api/templates/${tpl.uniqueName}")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        }
    }

    @Test
    fun duplicateName() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.EVERYONE)
        createTeam(tpl.uniqueName, "a")

        createTeam(tpl.uniqueName, "a") {
            Then {
                this.statusCode(HttpStatus.SC_CONFLICT)
            } Extract {
                this.body().`as`(ExceptionBody::class.java)
            } Let { resp ->
                resp.message shouldBe "team with name 'a' already exists for template '${tpl.uniqueName}'"
            }
        }
    }

    @Test
    fun addItemAsOwner() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.EVERYONE)
        createTeam(tpl.uniqueName, "a")
        val auth = login(tpl)

        uploadItem(auth, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)
    }

    @Test
    fun addItemAsTeam() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.EVERYONE)
        val team = createTeam(tpl.uniqueName, "a")
        val auth = login(team)

        val item = uploadItem(auth, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)

        item.team shouldBe team.name
    }

    @Test
    fun modifyItemAsTeam() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.EVERYONE)
        val team = createTeam(tpl.uniqueName, "a")
        val auth = login(team)

        val item = uploadItem(auth, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)

        Given {
            this.cookie(auth)
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(x = -1, y = 1, z = 0))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        }
    }

    @Test
    fun modifyItemAsOwner() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.EVERYONE)
        val team = createTeam(tpl.uniqueName, "a")
        val authTeam = login(team)
        val authOwner = login(tpl)

        val item = uploadItem(authTeam, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)

        Given {
            this.cookie(authOwner)
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(x = -1, y = 1, z = 0))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_OK)
        }
    }

    @Test
    fun modifyItemForbiddenForWrongTeam() {
        val tpl = createTemplate("tpl1", policy = TeamCreatePolicy.EVERYONE)
        val team1 = createTeam(tpl.uniqueName, "a")
        val team2 = createTeam(tpl.uniqueName, "b")
        val auth1 = login(team1)
        val auth2 = login(team2)

        val item = uploadItem(auth1, tpl.uniqueName, "i 1", 1, 2, 0, IMG1)

        Given {
            this.cookie(auth2)
            this.contentType(ContentType.JSON)
            this.body(TemplateItemUpdateDto(x = -1, y = 1, z = 0))
        } When {
            this.put("/api/templates/${tpl.uniqueName}/items/${item.id}/details")
        } Then {
            this.statusCode(HttpStatus.SC_FORBIDDEN)
        } Extract {
            this.body().`as`(ExceptionBody::class.java)
        } Let { resp ->
            resp.message shouldBe "modifying items is not permitted for you"
        }
    }
}
