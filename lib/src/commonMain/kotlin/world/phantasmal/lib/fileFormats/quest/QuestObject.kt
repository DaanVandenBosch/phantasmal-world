package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer

class QuestObject(var areaId: Int, val data: Buffer) {
    var type: ObjectType = TODO()
    val scriptLabel: Int? = TODO()
    val scriptLabel2: Int? = TODO()

    init {
        require(data.size == OBJECT_BYTE_SIZE) {
            "Data size should be $OBJECT_BYTE_SIZE but was ${data.size}."
        }
    }
}
