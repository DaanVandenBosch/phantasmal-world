package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer
import kotlin.math.roundToInt

class QuestNpc(var episode: Episode, var areaId: Int, val data: Buffer) {
    /**
     * Only seems to be valid for non-enemies.
     */
    var scriptLabel: Int
        get() = data.getF32(60u).roundToInt()
        set(value) {
            data.setF32(60u, value.toFloat())
        }

    var skin: Int
        get() = data.getI32(64u)
        set(value) {
            data.setI32(64u, value)
        }

    init {
        require(data.size == NPC_BYTE_SIZE) {
            "Data size should be $NPC_BYTE_SIZE but was ${data.size}."
        }
    }
}
