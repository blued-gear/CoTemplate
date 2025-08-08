package apps.chocolatecakecodes.cotemplate.dto

import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateDetailsDto(
    val name: String,
    val createdAt: Long,
    val teamCreatePolicy: TeamCreatePolicy,
    val width: Int,
    val height: Int,
    val templateCount: Int,
) {
    constructor() : this("", 0, TeamCreatePolicy.EVERYONE, 0, 0, 0)
}
