package apps.chocolatecakecodes.cotemplate.dto.export

import kotlinx.serialization.Serializable

@Serializable
internal data class CoTemplateDto(
    val template: TemplateDto,
    val users: List<UserDto>,
    val items: List<ItemDto>,
)
