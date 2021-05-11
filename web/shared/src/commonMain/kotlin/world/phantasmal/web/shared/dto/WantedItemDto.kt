package world.phantasmal.web.shared.dto

import kotlinx.serialization.Serializable

@Serializable
class WantedItemDto(
    val itemTypeId: Int,
    val amount: Int,
)
