package apps.chocolatecakecodes.cotemplate.auth

import java.security.Principal

data class TemplatePrincipal(
    val template: String,
    val user: String,
) : Principal {

    companion object {

        fun parse(principal: String): TemplatePrincipal {
            return principal.split("/").let {
                TemplatePrincipal(it[0], it[1])
            }
        }
    }

    override fun getName(): String {
        return "$template/$user"
    }
}
