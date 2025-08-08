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

@ApplicationScoped
internal class AuthLoginProvider(
    private val passwordService: PasswordService,
) : IdentityProvider<UsernamePasswordAuthenticationRequest> {

    override fun getRequestType(): Class<UsernamePasswordAuthenticationRequest> {
        return UsernamePasswordAuthenticationRequest::class.java
    }

    override fun authenticate(req: UsernamePasswordAuthenticationRequest, ctx: AuthenticationRequestContext): Uni<SecurityIdentity> {
        return ctx.runBlocking {
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
