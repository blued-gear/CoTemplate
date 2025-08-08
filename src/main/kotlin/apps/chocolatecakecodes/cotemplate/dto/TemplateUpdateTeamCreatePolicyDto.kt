package apps.chocolatecakecodes.cotemplate.dto

import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateUpdateTeamCreatePolicyDto(
    val policy: TeamCreatePolicy,
) {
    constructor() : this(TeamCreatePolicy.EVERYONE)
}
