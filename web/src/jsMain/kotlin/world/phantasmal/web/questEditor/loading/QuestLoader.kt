package world.phantasmal.web.questEditor.loading

import org.khronos.webgl.ArrayBuffer
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.quest.*
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.loading.LoadingCache
import world.phantasmal.webui.DisposableContainer

class QuestLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private val cache = addDisposable(
        LoadingCache<String, ArrayBuffer>(
            { path -> assetLoader.loadArrayBuffer("/quests$path") },
            { /* Nothing to dispose. */ }
        )
    )

    suspend fun loadDefaultQuest(episode: Episode): Quest {
        val ver = episode.toInt()
        return loadQuest("/defaults/default_ep_$ver.qst")
    }

    suspend fun loadCityQuest(episode: Episode): Quest {
        val ver = episode.toInt()
        return loadQuest("/city/city_ep_$ver.qst")
    }

    suspend fun loadLobbyQuest(variant: Int): Quest =
        loadQuest("/lobby/lobby_${variant.toString().padStart(2, '0')}.qst")

    private suspend fun loadQuest(path: String): Quest =
        parseQstToQuest(cache.get(path).cursor(Endianness.Little)).unwrap().quest
}
