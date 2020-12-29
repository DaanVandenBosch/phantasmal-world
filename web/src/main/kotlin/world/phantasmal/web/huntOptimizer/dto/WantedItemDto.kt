package world.phantasmal.web.huntOptimizer.dto

import kotlinx.serialization.Serializable

@Serializable
class WantedItemDto(
    val itemTypeId: Int,
    val amount: Int,
)
