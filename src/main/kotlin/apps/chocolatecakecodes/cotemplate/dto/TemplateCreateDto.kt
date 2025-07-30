package apps.chocolatecakecodes.cotemplate.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateCreateDto(
    val name: String,
    val width: Int,
    val height: Int,
) {
    constructor() : this("", 0, 0)
}
