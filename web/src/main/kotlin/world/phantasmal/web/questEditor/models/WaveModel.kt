package world.phantasmal.web.questEditor.models

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

class WaveModel(id: Int, areaId: Int, sectionId: Int) {
    private val _id = mutableVal(id)
    private val _areaId = mutableVal(areaId)
    private val _sectionId = mutableVal(sectionId)

    val id: Val<Int> = _id
    val areaId: Val<Int> = _areaId
    val sectionId: Val<Int> = _sectionId
}
