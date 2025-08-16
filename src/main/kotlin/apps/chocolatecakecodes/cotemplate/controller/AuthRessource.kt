package apps.chocolatecakecodes.cotemplate.controller

import apps.chocolatecakecodes.cotemplate.auth.CotemplateSecurityIdentity
import apps.chocolatecakecodes.cotemplate.dto.UserInfo
import apps.chocolatecakecodes.cotemplate.dto.UserInfoDto
import io.quarkus.security.identity.SecurityIdentity
import io.vertx.core.http.HttpServerResponse
import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Context
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.openapi.annotations.Operation
import org.jboss.resteasy.reactive.ResponseStatus
import org.jboss.resteasy.reactive.RestResponse

@Path("api/auth")
internal class AuthRessource(
    @param:ConfigProperty(name = "quarkus.http.auth.form.cookie-name")
    private val authCookieName: String,
) {

    @GET
    @Path("id")
    @PermitAll
    @Operation(
        operationId = "getUserInfo",
        summary = "returns information about the current user",
        description = "info: is logged in, role, team name, associated template"
    )
    fun getUserInfo(@Context auth: SecurityIdentity): UserInfoDto {
        val identity = CotemplateSecurityIdentity.parse(auth)
        return if(identity.isAnonymous) {
            UserInfoDto(true, null)
        } else {
            UserInfoDto(false, UserInfo(identity.template, identity.userName, identity.role))
        }
    }

    @POST
    @Path("logout")
    @ResponseStatus(RestResponse.StatusCode.NO_CONTENT)
    @PermitAll
    @Operation(
        operationId = "logout",
        summary = "logs the current user out",
    )
    fun logout(resp: HttpServerResponse) {
        resp.removeCookies(authCookieName, true)
    }
}
