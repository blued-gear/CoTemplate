package apps.chocolatecakecodes.cotemplate.auth

import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.service.PasswordService
import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest
import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext

@ApplicationScoped
internal class AuthProvider(
    private val passwordService: PasswordService,
) : IdentityProvider<UsernamePasswordAuthenticationRequest> {

    companion object {
        const val ATTRIBUTE_TEMPLATE = "cotemplate.UserOfTemplate"
    }

    override fun getRequestType(): Class<UsernamePasswordAuthenticationRequest> {
        return UsernamePasswordAuthenticationRequest::class.java
    }

    override fun authenticate(req: UsernamePasswordAuthenticationRequest, ctx: AuthenticationRequestContext): Uni<SecurityIdentity> {
        return ctx.runBlocking {
            val reqData = req.attributes["quarkus.http.routing.context"] as RoutingContext
            val templateName = reqData.request().formAttributes()["template"]

            val user = retrieveUser(req, templateName)

            return@runBlocking QuarkusSecurityIdentity.builder().apply {
                this.setPrincipal(QuarkusPrincipal(req.username))
                this.addRole(user.role.name)
                this.addAttribute(ATTRIBUTE_TEMPLATE, templateName)
                //this.addCredential(req.password)
                this.setAnonymous(false)
            }.build()
        }
    }

    @ActivateRequestContext
    protected fun retrieveUser(req: UsernamePasswordAuthenticationRequest, templateName: String): UserEntity {
        val template = TemplateEntity.findByUniqueName(templateName)
            ?: throw AuthenticationFailedException("template '$templateName' does not exist")
        val user = UserEntity.findByTemplateAndName(template, req.username)
            ?: throw AuthenticationFailedException("user '${req.username}' does not exist for template '$template'")
        if(!passwordService.checkPassword(String(req.password.password), user.pass))
            throw AuthenticationFailedException("invalid password")
        return user
    }
}
