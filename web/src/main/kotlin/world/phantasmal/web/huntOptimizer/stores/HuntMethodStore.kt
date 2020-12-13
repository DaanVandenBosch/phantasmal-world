package world.phantasmal.web.huntOptimizer.stores

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.web.core.IoDispatcher
import world.phantasmal.web.core.UiDispatcher
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.models.Server
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.dto.QuestDto
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import world.phantasmal.web.huntOptimizer.models.SimpleQuestModel
import world.phantasmal.web.huntOptimizer.persistence.HuntMethodPersister
import world.phantasmal.webui.stores.Store
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.time.Duration
import kotlin.time.minutes

class HuntMethodStore(
    private val uiStore: UiStore,
    private val assetLoader: AssetLoader,
    private val huntMethodPersister: HuntMethodPersister,
) : Store() {
    private val _methods = mutableListVal<HuntMethodModel>()

    val methods: ListVal<HuntMethodModel> by lazy {
        observe(uiStore.server) { loadMethods(it) }
        _methods
    }

    suspend fun setMethodTime(method: HuntMethodModel, time: Duration) {
        method.setUserTime(time)
        huntMethodPersister.persistMethodUserTimes(methods.value, uiStore.server.value)
    }

    private fun loadMethods(server: Server) {
        scope.launch(IoDispatcher) {
            val quests = assetLoader.load<List<QuestDto>>("/quests.${server.slug}.json")

            val methods = quests
                .asSequence()
                .filter {
                    when (it.id) {
                        // The following quests are left out because their enemies don't drop
                        // anything.
                        31, // Black Paper's Dangerous Deal
                        34, // Black Paper's Dangerous Deal 2
                        1305, // Maximum Attack S (Ep. 1)
                        1306, // Maximum Attack S (Ep. 2)
                        1307, // Maximum Attack S (Ep. 4)
                        313, // Beyond the Horizon

                            // MAXIMUM ATTACK 3 Ver2 is filtered out because its actual enemy
                            // count depends on the path taken.
                            // TODO: Generate a method per path for MA3v2.
                        314,
                        -> false

                        else -> true
                    }
                }
                .map { quest ->
                    var totalEnemyCount = 0
                    val enemyCounts = mutableMapOf<NpcType, Int>()

                    for ((code, count) in quest.enemy_counts) {
                        val npcType = NpcType.valueOf(code)

                        enemyCounts[npcType] = count
                        totalEnemyCount += count
                    }

                    val duration = when {
                        quest.name.matches(Regex("""^\d-\d.*""")) ->
                            DEFAULT_GOVERNMENT_TEST_DURATION

                        totalEnemyCount > 400 ->
                            DEFAULT_LARGE_ENEMY_COUNT_DURATION

                        else ->
                            DEFAULT_DURATION
                    }

                    HuntMethodModel(
                        "q${quest.id}",
                        quest.name,
                        SimpleQuestModel(
                            quest.id,
                            quest.name,
                            Episode.fromInt(quest.episode),
                            enemyCounts
                        ),
                        duration
                    )
                }
                .toList()

            huntMethodPersister.loadMethodUserTimes(methods, server)

            withContext(UiDispatcher) {
                _methods.replaceAll(methods)
            }
        }
    }

    companion object {
        private val DEFAULT_DURATION = 30.minutes
        private val DEFAULT_GOVERNMENT_TEST_DURATION = 45.minutes
        private val DEFAULT_LARGE_ENEMY_COUNT_DURATION = 45.minutes
    }
}
