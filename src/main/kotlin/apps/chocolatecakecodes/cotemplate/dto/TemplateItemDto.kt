package apps.chocolatecakecodes.cotemplate.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateItemDto(
    val id: String,
    val description: String,
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
    val z: Int,
) {
    constructor() : this("0", "", 0, 0, 0, 0, 0)
}
