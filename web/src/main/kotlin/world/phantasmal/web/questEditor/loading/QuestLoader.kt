package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.CoroutineScope
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.lib.fileFormats.quest.Quest
import world.phantasmal.lib.fileFormats.quest.parseQstToQuest
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.webui.DisposableContainer

class QuestLoader(
    scope: CoroutineScope,
    private val assetLoader: AssetLoader,
) : DisposableContainer() {
    private val cache = addDisposable(
        LoadingCache<String, ArrayBuffer>(
            scope,
            { path -> assetLoader.loadArrayBuffer("/quests$path") },
            { /* Nothing to dispose. */ }
        )
    )

    suspend fun loadDefaultQuest(episode: Episode): Quest {
        require(episode == Episode.I) {
            "Episode $episode not yet supported."
        }

        return loadQuest("/defaults/default_ep_1.qst")
    }

    private suspend fun loadQuest(path: String): Quest =
        parseQstToQuest(cache.get(path).cursor(Endianness.Little)).unwrap().quest
}
