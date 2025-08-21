package apps.chocolatecakecodes.cotemplate.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class TemplatesDto(
    val templates: Map<String, TemplateDetailsDto>
)
