package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.lib.fileFormats.quest.Quest
import world.phantasmal.lib.fileFormats.quest.parseQstToQuest
import world.phantasmal.web.core.loading.AssetLoader

class QuestLoader(
    private val scope: CoroutineScope,
    private val assetLoader: AssetLoader,
) : TrackedDisposable() {
    private val cache = LoadingCache<String, ArrayBuffer>()

    override fun internalDispose() {
        cache.dispose()
        super.internalDispose()
    }

    suspend fun loadDefaultQuest(episode: Episode): Quest {
        require(episode == Episode.I) {
            "Episode $episode not yet supported."
        }

        return loadQuest("/defaults/default_ep_1.qst")
    }

    private suspend fun loadQuest(path: String): Quest {
        val buffer = cache.getOrPut(path) {
            scope.async {
                assetLoader.loadArrayBuffer("/quests$path")
            }
        }.await()

        return parseQstToQuest(buffer.cursor(Endianness.Little)).unwrap().quest
    }
}
