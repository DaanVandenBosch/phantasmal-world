package world.phantasmal.web.viewer.loading

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.ninja.NjMotion
import world.phantasmal.psolib.fileFormats.ninja.parseNjm
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.questEditor.loading.LoadingCache
import world.phantasmal.webui.DisposableContainer

class AnimationAssetLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private val ninjaMotionCache: LoadingCache<String, NjMotion> =
        addDisposable(LoadingCache(::loadNinjaMotion) { /* Nothing to dispose. */ })

    suspend fun loadAnimation(filePath: String): NjMotion =
        ninjaMotionCache.get(filePath)

    private suspend fun loadNinjaMotion(filePath: String): NjMotion =
        parseNjm(assetLoader.loadArrayBuffer(filePath).cursor(Endianness.Little))
}
