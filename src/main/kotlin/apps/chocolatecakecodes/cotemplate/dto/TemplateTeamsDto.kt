package apps.chocolatecakecodes.cotemplate.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateTeamsDto(
    val teams: List<String>
) {
    constructor() : this(emptyList())
}
