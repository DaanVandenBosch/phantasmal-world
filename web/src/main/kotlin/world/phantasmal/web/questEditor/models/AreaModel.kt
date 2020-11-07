package world.phantasmal.web.questEditor.models

import world.phantasmal.core.requireNonNegative

class AreaModel(
    /**
     * Matches the PSO ID.
     */
    val id: Int,
    val name: String,
    val order: Int,
    val areaVariants: List<AreaVariantModel>,
) {
    init {
        requireNonNegative(id, "id")
    }
}
