package world.phantasmal.web.viewer.controllers

import world.phantasmal.observable.value.Val
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.controllers.PathAwareTab
import world.phantasmal.web.core.controllers.PathAwareTabContainerController
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.viewer.ViewerUrls
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.web.viewer.stores.ViewerStore

sealed class ViewerTab(
    override val title: String,
    override val path: String,
) : PathAwareTab {
    object Mesh : ViewerTab("Model", ViewerUrls.mesh)
    object Texture : ViewerTab("Textures", ViewerUrls.texture)
}

class ViewerController(
    uiStore: UiStore,
    private val store: ViewerStore,
) : PathAwareTabContainerController<ViewerTab>(
    uiStore,
    PwToolType.Viewer,
    tabs = listOf(ViewerTab.Mesh, ViewerTab.Texture),
) {
    val characterClasses: List<CharacterClass> = CharacterClass.VALUES_LIST
    val currentCharacterClass: Val<CharacterClass?> = store.currentCharacterClass

    suspend fun setCurrentCharacterClass(char: CharacterClass?) {
        store.setCurrentCharacterClass(char)
    }
}
