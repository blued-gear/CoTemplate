package apps.chocolatecakecodes.cotemplate.auth

import io.quarkus.security.credential.Credential
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import java.security.Permission
import java.security.Principal

internal class CotemplateSecurityIdentity private constructor(
    val userId: Long,
    val userName: String,
    val role: String,
    val template: String,
    var anonymous: Boolean,
) : SecurityIdentity {

    companion object {

        const val ATTRIBUTE_TEMPLATE = "cotemplate.UserOfTemplate"
        const val ATTRIBUTE_USER_ID = "cotemplate.UserId"
        const val ATTRIBUTE_USER_NAME = "cotemplate.UserName"

        fun parse(identity: SecurityIdentity): CotemplateSecurityIdentity {
            return if(identity.isAnonymous) {
                CotemplateSecurityIdentity()
            } else {
                CotemplateSecurityIdentity(
                    identity.getAttribute(ATTRIBUTE_USER_ID),
                    identity.getAttribute(ATTRIBUTE_USER_NAME),
                    identity.roles.first(),
                    identity.getAttribute(ATTRIBUTE_TEMPLATE),
                )
            }
        }
    }

    private val attributes: Map<String, Any>

    constructor(
        userId: Long,
        userName: String,
        role: String,
        template: String,
    ) : this(userId, userName, role, template, false)

    constructor() : this(0, "", "_GUEST", "", true)

    init {
        attributes = mapOf(
            Pair(ATTRIBUTE_TEMPLATE, template),
            Pair(ATTRIBUTE_USER_ID, userId),
            Pair(ATTRIBUTE_USER_NAME, userName),
        )
    }

    override fun checkPermission(permission: Permission?): Uni<Boolean?> {
        return Uni.createFrom().item(false)
    }

    override fun getAttributes(): Map<String, Any> {
        return attributes
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getAttribute(name: String): T? {
        return attributes[name] as T?
    }

    override fun getCredentials(): Set<Credential> {
        return emptySet()
    }

    override fun <T : Credential> getCredential(credentialType: Class<T>): T? {
        return null
    }

    override fun hasRole(role: String): Boolean {
        return this.role == role
    }

    override fun getRoles(): Set<String> {
        return setOf(role)
    }

    override fun isAnonymous(): Boolean {
        return anonymous
    }

    override fun getPrincipal(): Principal {
        return TemplatePrincipal(template, userName)
    }
}
