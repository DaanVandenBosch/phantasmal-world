package world.phantasmal.web.questEditor.models

import world.phantasmal.core.requireNonNegative

class AreaModel(
    /**
     * Matches the PSO ID.
     */
    val id: Int,
    val name: String,
    val bossArea: Boolean,
    val order: Int,
    val areaVariants: List<AreaVariantModel>,
) {
    init {
        requireNonNegative(id, "id")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false
        return id == (other as AreaModel).id
    }

    override fun hashCode(): Int = id
}
