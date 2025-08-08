package apps.chocolatecakecodes.cotemplate.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TeamCreatedDto(
    val template: String,
    val name: String,
    val password: String,
) {
    constructor() : this("", "", "")
}
