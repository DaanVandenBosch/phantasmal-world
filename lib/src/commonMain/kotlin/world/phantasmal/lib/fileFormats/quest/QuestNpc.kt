package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer
import kotlin.math.roundToInt

class QuestNpc(var episode: Episode, var areaId: Int, val data: Buffer) {
    /**
     * Only seems to be valid for non-enemies.
     */
    var scriptLabel: Int
        get() = data.getFloat(60).roundToInt()
        set(value) {
            data.setFloat(60, value.toFloat())
        }

    var skin: Int
        get() = data.getInt(64)
        set(value) {
            data.setInt(64, value)
        }

    init {
        require(data.size == NPC_BYTE_SIZE) {
            "Data size should be $NPC_BYTE_SIZE but was ${data.size}."
        }
    }
}
