package world.phantasmal.web.huntoptimizer.models

import world.phantasmal.lib.fileformats.quest.Episode
import world.phantasmal.lib.fileformats.quest.NpcType
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
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
    private val _userTime = mutableVal<Duration?>(null)

    val episode: Episode = quest.episode

    val enemyCounts: Map<NpcType, Int> = quest.enemyCounts

    /**
     * The time it takes to complete the quest in hours as specified by the user.
     */
    val userTime: Val<Duration?> = _userTime

    val time: Val<Duration> = userTime.transform { it ?: defaultTime }

    fun setUserTime(userTime: Duration?): HuntMethodModel {
        _userTime.value = userTime
        return this
    }
}
