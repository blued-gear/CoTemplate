package apps.chocolatecakecodes.cotemplate

import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest
import io.quarkus.security.runtime.QuarkusPrincipal
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.inject.Singleton

@Singleton
internal class DummyAuth : IdentityProvider<UsernamePasswordAuthenticationRequest> {

    override fun getRequestType(): Class<UsernamePasswordAuthenticationRequest> {
        return UsernamePasswordAuthenticationRequest::class.java
    }

    override fun authenticate(req: UsernamePasswordAuthenticationRequest, ctx: AuthenticationRequestContext): Uni<SecurityIdentity> {
        return QuarkusSecurityIdentity.builder().apply {
            this.setPrincipal(QuarkusPrincipal("dummy"))
            this.addCredential(req.password)
            this.setAnonymous(false)
        }.build().let { Uni.createFrom().item(it) }
    }
}
