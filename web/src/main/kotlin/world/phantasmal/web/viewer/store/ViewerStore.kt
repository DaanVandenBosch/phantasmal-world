package world.phantasmal.web.viewer.store

import mu.KotlinLogging
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.core.models.SectionId
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.webui.stores.Store

private val logger = KotlinLogging.logger {}

class ViewerStore(private val assetLoader: CharacterClassAssetLoader) : Store() {
    private val _currentNinjaObject = mutableVal<NinjaObject<*>?>(null)
    private val _currentTextures = mutableListVal<XvrTexture?>(mutableListOf())
    private val _currentCharacterClass = mutableVal<CharacterClass?>(null)

    val currentNinjaObject: Val<NinjaObject<*>?> = _currentNinjaObject
    val currentTextures: ListVal<XvrTexture?> = _currentTextures
    val currentCharacterClass: Val<CharacterClass?> = _currentCharacterClass

    fun setCurrentNinjaObject(ninjaObject: NinjaObject<*>?) {
        if (_currentCharacterClass.value != null) {
            _currentCharacterClass.value = null
            _currentTextures.clear()
        }

        _currentNinjaObject.value = ninjaObject
    }

    fun setCurrentTextures(textures: List<XvrTexture>) {
        _currentTextures.value = textures
    }

    suspend fun setCurrentCharacterClass(char: CharacterClass?) {
        _currentCharacterClass.value = char
        loadCharacterClassNinjaObject()
    }

    private suspend fun loadCharacterClassNinjaObject() {
        val char = currentCharacterClass.value
            ?: return

        try {
            val ninjaObject = assetLoader.loadNinjaObject(char)
            val textures = assetLoader.loadXvrTextures(char, SectionId.Whitill, 0)
            _currentNinjaObject.value = ninjaObject
            _currentTextures.replaceAll(textures)
        } catch (e: Exception) {
            logger.error(e) { "Couldn't load Ninja model for $char." }

            _currentNinjaObject.value = null
            _currentTextures.clear()
        }
    }
}
