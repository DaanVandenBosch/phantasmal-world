package world.phantasmal.web.questEditor.models

import world.phantasmal.core.requireNonNegative
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal

class AreaVariantModel(val id: Int, val area: AreaModel) {
    private val _sections = mutableListVal<SectionModel>()

    val sections: ListVal<SectionModel> = _sections

    init {
        requireNonNegative(id, "id")
    }

    fun setSections(sections: List<SectionModel>) {
        _sections.replaceAll(sections)
    }
}
