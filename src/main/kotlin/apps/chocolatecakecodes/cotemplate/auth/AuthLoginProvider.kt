package apps.chocolatecakecodes.cotemplate.auth

import apps.chocolatecakecodes.cotemplate.db.TemplateEntity
import apps.chocolatecakecodes.cotemplate.db.UserEntity
import apps.chocolatecakecodes.cotemplate.service.PasswordService
import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest
import io.smallrye.mutiny.Uni
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.control.ActivateRequestContext
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
internal class AuthLoginProvider(
    private val passwordService: PasswordService,
    @param:ConfigProperty(name = "cotemplate.auth.admin-password")
    private val adminPassword: String,
) : IdentityProvider<UsernamePasswordAuthenticationRequest> {

    companion object {

        const val ADMIN_USER_NAME = "admin"
    }

    private val adminPasswordChars = adminPassword.toCharArray()

    override fun getRequestType(): Class<UsernamePasswordAuthenticationRequest> {
        return UsernamePasswordAuthenticationRequest::class.java
    }

    override fun authenticate(req: UsernamePasswordAuthenticationRequest, ctx: AuthenticationRequestContext): Uni<SecurityIdentity> {
        return ctx.runBlocking {
            tryAdminAuth(req)?.let { return@runBlocking it }

            val reqData = req.attributes["quarkus.http.routing.context"] as RoutingContext
            val templateName = reqData.request().formAttributes()["template"]
            val user = retrieveUser(req, templateName)

            return@runBlocking CotemplateSecurityIdentity(
                user.id!!,
                user.name,
                user.role,
                templateName,
            )
        }
    }

    private fun tryAdminAuth(req: UsernamePasswordAuthenticationRequest): CotemplateSecurityIdentity? {
        if(req.username != ADMIN_USER_NAME)
            return null
        if(adminPassword == "" || adminPassword == "_")
            throw AuthenticationFailedException("admin account is disabled")

        if(!req.password.password.contentEquals(adminPasswordChars))
            throw AuthenticationFailedException("invalid password")

        return CotemplateSecurityIdentity(0, ADMIN_USER_NAME, Role.ADMIN, ".")
    }

    @ActivateRequestContext
    protected fun retrieveUser(req: UsernamePasswordAuthenticationRequest, templateName: String): UserEntity {
        val template = TemplateEntity.findByUniqueName(templateName)
            ?: throw AuthenticationFailedException("template '$templateName' does not exist")
        val user = UserEntity.findByTemplateAndName(template, req.username)
            ?: throw AuthenticationFailedException("user '${req.username}' does not exist for template '${template.uniqueName}'")
        if(!passwordService.checkPassword(String(req.password.password), user.pass))
            throw AuthenticationFailedException("invalid password")
        return user
    }
}
