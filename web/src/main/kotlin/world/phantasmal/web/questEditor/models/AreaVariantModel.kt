package world.phantasmal.web.questEditor.models

import world.phantasmal.core.requireNonNegative
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal

class AreaVariantModel(val id: Int, val area: AreaModel) {
    private val _sections = mutableListVal<SectionModel>()

    // Exception for Seaside Area at Night, variant 1.
    // Phantasmal World 4 and Lost heart breaker use this to have two tower maps.
    val name: String = if (area.id == 16 && id == 1) "West Tower" else area.name

    val sections: ListVal<SectionModel> = _sections

    init {
        requireNonNegative(id, "id")
    }

    fun setSections(sections: List<SectionModel>) {
        _sections.replaceAll(sections)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false
        other as AreaVariantModel
        return id == other.id && area.id == other.area.id
    }

    override fun hashCode(): Int = 31 * id + area.hashCode()
}
