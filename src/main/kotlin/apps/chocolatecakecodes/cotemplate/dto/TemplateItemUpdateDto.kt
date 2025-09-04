package apps.chocolatecakecodes.cotemplate.dto

import apps.chocolatecakecodes.cotemplate.service.TemplateItemService
import kotlinx.serialization.Serializable
import org.hibernate.validator.constraints.Length

@Serializable
internal data class TemplateItemUpdateDto(
    @field:Length(max = TemplateItemService.DESCRIPTION_MAX_LEN)
    val description: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val z: Int? = null,
)
