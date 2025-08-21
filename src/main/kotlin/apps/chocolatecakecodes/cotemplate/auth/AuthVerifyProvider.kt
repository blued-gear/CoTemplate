package apps.chocolatecakecodes.cotemplate.auth

import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.TrustedAuthenticationRequest
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import jakarta.ws.rs.InternalServerErrorException

@ApplicationScoped
internal class AuthVerifyProvider : IdentityProvider<TrustedAuthenticationRequest> {

    override fun getRequestType(): Class<TrustedAuthenticationRequest> {
        return TrustedAuthenticationRequest::class.java
    }

    override fun authenticate(req: TrustedAuthenticationRequest, ctx: AuthenticationRequestContext): Uni<SecurityIdentity> {
        return ctx.runBlocking {
            val principal = TemplatePrincipal.parse(req.principal)

            if(principal.user == AuthLoginProvider.ADMIN_USER_NAME) {
                return@runBlocking CotemplateSecurityIdentity(
                    0,
                    principal.user,
                    Role.ADMIN,
                    principal.template,
                )
            } else {
                val user = retrieveUser(principal)
                return@runBlocking CotemplateSecurityIdentity(
                    user.id!!,
                    user.name,
                    user.role,
                    principal.template,
                )
            }
        }
    }

    @ActivateRequestContext
    protected fun retrieveUser(principal: TemplatePrincipal): UserEntity {
        val template = TemplateEntity.findByUniqueName(principal.template)
            ?: throw InternalServerErrorException("template of principal not found ($principal)")
        val user = UserEntity.findByTemplateAndName(template, principal.user)
            ?: throw InternalServerErrorException("user of principal not found ($principal)")
        return user
    }
}
