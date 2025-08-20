package apps.chocolatecakecodes.cotemplate.dto.export

import kotlinx.serialization.Serializable

@Serializable
internal data class UserDto(
    val name: String,
    val pass: String,
    val role: String,
)
