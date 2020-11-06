package world.phantasmal.web.viewer.store

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.webui.stores.Store

class ViewerStore(scope: CoroutineScope) : Store(scope) {
    private val _currentNinjaObject = mutableVal<NinjaObject<*>?>(null)

    val currentNinjaObject: Val<NinjaObject<*>?> = _currentNinjaObject

    fun setCurrentNinjaObject(ninjaObject: NinjaObject<*>?) {
        _currentNinjaObject.value = ninjaObject
    }
}
