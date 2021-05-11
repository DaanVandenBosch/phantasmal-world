package world.phantasmal.web.huntOptimizer.models

import world.phantasmal.lib.Episode
import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.observable.cell.orElse
import kotlin.time.Duration

class HuntMethodModel(
    val id: String,
    val name: String,
    val quest: SimpleQuestModel,
    /**
     * The estimated time it takes to complete the quest in hours.
     */
    val defaultTime: Duration,
) {
    private val _userTime = mutableCell<Duration?>(null)

    val episode: Episode = quest.episode

    val enemyCounts: Map<NpcType, Int> = quest.enemyCounts

    /**
     * The time it takes to complete the quest in hours as specified by the user.
     */
    val userTime: Cell<Duration?> = _userTime

    val time: Cell<Duration> = userTime.orElse { defaultTime }

    fun setUserTime(userTime: Duration?) {
        _userTime.value = userTime
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false
        return id == (other as HuntMethodModel).id
    }

    override fun hashCode(): Int = id.hashCode()
}
