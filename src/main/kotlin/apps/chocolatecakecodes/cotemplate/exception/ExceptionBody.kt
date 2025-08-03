package apps.chocolatecakecodes.cotemplate.exception

import kotlinx.serialization.Serializable

@Serializable
internal data class ExceptionBody(
    val message: String,
) {
    constructor() : this("")
}
