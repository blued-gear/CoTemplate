package apps.chocolatecakecodes.cotemplate.dto

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
internal data class UserInfoDto(
    @field:JsonProperty("isGuest")
    val isGuest: Boolean,
    val info: UserInfo?
) {
    constructor() : this(true, null)
}

@Serializable
internal data class UserInfo(
    val template: String,
    val team: String,
    val role: String,
) {
    constructor() : this("", "", "")
}
