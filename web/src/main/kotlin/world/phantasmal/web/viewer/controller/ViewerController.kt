package world.phantasmal.web.viewer.controller

import world.phantasmal.observable.value.Val
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.web.viewer.store.ViewerStore
import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabContainerController

sealed class ViewerTab(override val title: String) : Tab {
    object Mesh : ViewerTab("Model")
    object Texture : ViewerTab("Texture")
}

class ViewerController(private val store: ViewerStore) : TabContainerController<ViewerTab>(
    tabs = listOf(ViewerTab.Mesh, ViewerTab.Texture)
) {
    val characterClasses: List<CharacterClass> = CharacterClass.VALUES
    val currentCharacterClass: Val<CharacterClass?> = store.currentCharacterClass

    suspend fun setCurrentCharacterClass(char: CharacterClass?) {
        store.setCurrentCharacterClass(char)
    }
}
