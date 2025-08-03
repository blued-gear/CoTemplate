package apps.chocolatecakecodes.cotemplate.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateDetailsDto(
    val name: String,
    val createdAt: Long,
    val width: Int,
    val height: Int,
    val templateCount: Int,
) {
    constructor() : this("", 0, 0, 0, 0)
}
