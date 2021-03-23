package world.phantasmal.web.viewer.controllers

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.plus
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.viewer.stores.ViewerStore
import world.phantasmal.webui.controllers.Controller

class CharacterClassOptionsController(private val store: ViewerStore) : Controller() {
    val enabled = store.currentCharacterClass.isNotNull()
    val currentSectionId: Val<SectionId> = store.currentSectionId
    val currentBodyOptions: Val<List<Int>> = store.currentCharacterClass.map { char ->
        if (char == null) emptyList() else (1..char.bodyStyleCount).toList()
    }
    val currentBody: Val<Int> = store.currentBody + 1

    suspend fun setCurrentSectionId(sectionId: SectionId) {
        store.setCurrentSectionId(sectionId)
    }

    suspend fun setCurrentBody(body: Int) {
        store.setCurrentBody(body - 1)
    }
}
