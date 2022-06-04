package world.phantasmal.web.huntOptimizer.stores

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizerUrls.methods
import world.phantasmal.web.huntOptimizer.models.HuntMethodModel
import world.phantasmal.web.huntOptimizer.models.SimpleQuestModel
import world.phantasmal.web.huntOptimizer.persistence.HuntMethodPersister
import world.phantasmal.web.shared.dto.QuestDto
import world.phantasmal.webui.LoadingStatusCell
import world.phantasmal.webui.LoadingStatusCellImpl
import world.phantasmal.webui.stores.Store
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.time.Duration

class HuntMethodStore(
    private val uiStore: UiStore,
    private val assetLoader: AssetLoader,
    private val huntMethodPersister: HuntMethodPersister,
) : Store() {
    private val _methods = mutableListCell<HuntMethodModel>()
    private val _methodsStatus = LoadingStatusCellImpl(scope, "methods", ::loadMethods)

    /** Hunting methods supported by the current server. */
    val methods: ListCell<HuntMethodModel> by lazy {
        observeNow(uiStore.server) { _methodsStatus.load() }
        _methods
    }

    /** Loading status of [methods]. */
    val methodsStatus: LoadingStatusCell = _methodsStatus

    suspend fun setMethodTime(method: HuntMethodModel, time: Duration) {
        method.setUserTime(time)

        huntMethodPersister.persistMethodUserTimes(methods.value, uiStore.server.value)
    }

    private suspend fun loadMethods() {
        val server = uiStore.server.value

        withContext(Dispatchers.Default) {
            val quests: List<QuestDto> = assetLoader.load("/quests.${server.slug}.json")

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
                        quest.name.matches(GOVERNMENT_QUEST_NAME_REGEX) ->
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

            withContext(Dispatchers.Main) {
                _methods.replaceAll(methods)
            }
        }
    }

    companion object {
        private val GOVERNMENT_QUEST_NAME_REGEX = Regex("""^\d-\d.*""")
        private val DEFAULT_DURATION = Duration.minutes(30)
        private val DEFAULT_GOVERNMENT_TEST_DURATION = Duration.minutes(45)
        private val DEFAULT_LARGE_ENEMY_COUNT_DURATION = Duration.minutes(45)
    }
}
