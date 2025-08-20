package apps.chocolatecakecodes.cotemplate.dto.export

import kotlinx.serialization.Serializable

@Serializable
internal data class ItemDto(
    val imgRef: Int,
    val ownerRef: Int,
    val description: String,
    val x: Int,
    val y: Int,
    val z: Int,
)
