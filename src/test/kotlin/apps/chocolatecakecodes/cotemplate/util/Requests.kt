package apps.chocolatecakecodes.cotemplate.util

import apps.chocolatecakecodes.cotemplate.TemplateItemTest
import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import apps.chocolatecakecodes.cotemplate.dto.TeamCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreateDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateCreatedDto
import apps.chocolatecakecodes.cotemplate.dto.TemplateItemDto
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.restassured.http.ContentType
import io.restassured.http.Cookie
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import org.apache.http.HttpStatus

internal fun login(newTpl: TemplateCreatedDto): Cookie {
    return login(newTpl.uniqueName, newTpl.ownerUsername, newTpl.ownerPassword)
}

internal fun login(newTpl: TeamCreatedDto): Cookie {
    return login(newTpl.template, newTpl.name, newTpl.password)
}

internal fun login(tpl: String, uName: String, uPass: String): Cookie {
    var ret: Cookie? = null
    Given {
        this.formParam("username", uName)
        this.formParam("password", uPass)
        this.formParam("template", tpl)
    } When {
        this.post("/api/auth/login")
    } Then {
        this.statusCode(HttpStatus.SC_OK)
    } Extract {
        ret = this.detailedCookie("quarkus-credential")
    }
    return ret!!
}

internal fun createTemplate(name: String = "tpl1", w: Int = 256, h: Int = 128, policy: TeamCreatePolicy = TeamCreatePolicy.EVERYONE): TemplateCreatedDto {
    return createTemplate(name, w, h, policy) {
        Then {
            this.statusCode(HttpStatus.SC_CREATED)
        } Extract {
            this.body().`as`(TemplateCreatedDto::class.java).also {
                it.uniqueName shouldEndWith "-$name"
                it.ownerUsername shouldNotBe ""
                it.ownerPassword shouldNotBe ""
            }
        }
    }
}

internal fun <R> createTemplate(name: String = "tpl1", w: Int = 256, h: Int = 128, policy: TeamCreatePolicy = TeamCreatePolicy.EVERYONE, validator: Response.() -> R): R {
    val req = Given {
        this.contentType(ContentType.JSON)
        this.body(TemplateCreateDto(name, w, h, policy))
    } When {
        this.post("/api/templates")
    }
    return validator(req)
}

internal fun createTeam(template: String, name: String, auth: Cookie? = null): TeamCreatedDto {
    return createTeam(template, name, auth) {
        Then {
            this.statusCode(HttpStatus.SC_CREATED)
        } Extract {
            this.body().`as`(TeamCreatedDto::class.java)
        }
    }
}

internal fun <R> createTeam(template: String, name: String, auth: Cookie? = null, validator: Response.() -> R): R {
    val req = Given {
        if(auth != null) this.cookie(auth)
        return@Given this
    } When {
        this.post("/api/templates/$template/teams/$name")
    }
    return validator(req)
}

internal fun uploadItem(auth: Cookie, tpl: String, desc: String, x: Int, y: Int, z: Int, classpath: String): TemplateItemDto {
    return Given {
        this.cookie(auth)
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
        this.post("/api/templates/$tpl/items")
    } Then {
        this.statusCode(HttpStatus.SC_CREATED)
    } Extract {
        this.body().`as`(TemplateItemDto::class.java)
    }
}
