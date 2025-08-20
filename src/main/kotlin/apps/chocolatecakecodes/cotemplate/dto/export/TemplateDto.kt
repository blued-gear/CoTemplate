package apps.chocolatecakecodes.cotemplate.dto.export

import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateDto(
    val name: String,
    val width: Int,
    val height: Int,
    val teamCreatePolicy: TeamCreatePolicy,
)
