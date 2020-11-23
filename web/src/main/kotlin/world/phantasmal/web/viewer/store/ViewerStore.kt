package world.phantasmal.web.viewer.store

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.webui.stores.Store

class ViewerStore(scope: CoroutineScope) : Store(scope) {
    private val _currentNinjaObject = mutableVal<NinjaObject<*>?>(null)
    private val _currentTextures = mutableListVal<XvrTexture>(mutableListOf())

    val currentNinjaObject: Val<NinjaObject<*>?> = _currentNinjaObject
    val currentTextures: ListVal<XvrTexture> = _currentTextures

    fun setCurrentNinjaObject(ninjaObject: NinjaObject<*>?) {
        _currentNinjaObject.value = ninjaObject
    }

    fun setCurrentTextures(textures: List<XvrTexture>) {
        _currentTextures.value = textures
    }
}
