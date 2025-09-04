package apps.chocolatecakecodes.cotemplate.dto

import apps.chocolatecakecodes.cotemplate.auth.TeamCreatePolicy
import apps.chocolatecakecodes.cotemplate.service.TemplateManagementService
import kotlinx.serialization.Serializable
import org.hibernate.validator.constraints.Length

@Serializable
internal data class TemplateCreateDto(
    @field:Length(min = TemplateManagementService.NAME_MIN_LENGTH, max = TemplateManagementService.NAME_MAX_LENGTH)
    val name: String,
    val width: Int,
    val height: Int,
    val teamCreatePolicy: TeamCreatePolicy,
) {
    constructor() : this("", 0, 0, TeamCreatePolicy.EVERYONE)
}
