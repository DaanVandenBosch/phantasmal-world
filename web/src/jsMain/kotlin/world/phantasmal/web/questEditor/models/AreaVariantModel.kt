package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.core.requireNonNegative
import world.phantasmal.psolib.Episode

class AreaVariantModel(val id: Int, val area: AreaModel, val episode: Episode) {
    private val _sections = mutableListCell<SectionModel>()

    val name: String = area.name

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
