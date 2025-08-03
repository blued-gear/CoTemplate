package apps.chocolatecakecodes.cotemplate.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TemplateItemUpdateDto(
    val description: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val z: Int? = null,
)
