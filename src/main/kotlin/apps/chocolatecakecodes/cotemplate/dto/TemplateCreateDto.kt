package apps.chocolatecakecodes.cotemplate.dto

import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateCreateDto(
    val name: String,
    val width: Int,
    val height: Int,
    val teamCreatePolicy: TeamCreatePolicy,
) {
    constructor() : this("", 0, 0, TeamCreatePolicy.EVERYONE)
}
