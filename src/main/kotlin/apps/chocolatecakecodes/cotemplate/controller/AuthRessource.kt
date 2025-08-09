package apps.chocolatecakecodes.cotemplate.controller

import apps.chocolatecakecodes.cotemplate.auth.CotemplateSecurityIdentity
import apps.chocolatecakecodes.cotemplate.dto.UserInfo
import apps.chocolatecakecodes.cotemplate.dto.UserInfoDto
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Context

@Path("api/auth")
internal class AuthRessource {

    @GET
    @Path("id")
    @PermitAll
    fun getUserInfo(@Context auth: SecurityIdentity): UserInfoDto {
        val identity = CotemplateSecurityIdentity.parse(auth)
        return if(identity.isAnonymous) {
            UserInfoDto(true, null)
        } else {
            UserInfoDto(false, UserInfo(identity.template, identity.userName, identity.role))
        }
    }

}
