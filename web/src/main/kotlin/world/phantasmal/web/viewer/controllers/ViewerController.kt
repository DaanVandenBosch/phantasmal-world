package world.phantasmal.web.viewer.controllers

import world.phantasmal.observable.value.Val
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.web.viewer.stores.ViewerStore
import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabContainerController

sealed class ViewerTab(override val title: String) : Tab {
    object Mesh : ViewerTab("Model")
    object Texture : ViewerTab("Texture")
}

class ViewerController(private val store: ViewerStore) : TabContainerController<ViewerTab>(
    tabs = listOf(ViewerTab.Mesh, ViewerTab.Texture)
) {
    val characterClasses: List<CharacterClass> = CharacterClass.VALUES_LIST
    val currentCharacterClass: Val<CharacterClass?> = store.currentCharacterClass

    suspend fun setCurrentCharacterClass(char: CharacterClass?) {
        store.setCurrentCharacterClass(char)
    }
}
