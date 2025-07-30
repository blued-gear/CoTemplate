package apps.chocolatecakecodes.cotemplate.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateCreatedDto(
    val uniqueName: String,
    val ownerUsername: String,
    val ownerPassword: String,
) {
    constructor() : this("", "", "")
}
