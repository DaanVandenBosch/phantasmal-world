package world.phantasmal.web.questEditor.models

import world.phantasmal.core.requireNonNegative
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.mutableListCell

class AreaVariantModel(val id: Int, val area: AreaModel) {
    private val _sections = mutableListCell<SectionModel>()

    // Tower has 5 variants. PW4 and LHB use variant 1. ID 0 is Seaside Area at Night.
    val name: String = if (area.id == 16 && id in 1..5) "West Tower" else area.name

    val sections: ListCell<SectionModel> = _sections

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
