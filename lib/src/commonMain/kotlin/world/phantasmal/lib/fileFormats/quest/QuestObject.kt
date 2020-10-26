package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer

class QuestObject(var areaId: Int, val data: Buffer) {
    var type: ObjectType
        get() = TODO()
        set(_) = TODO()
    val scriptLabel: Int? = null // TODO Implement scriptLabel.
    val scriptLabel2: Int? = null // TODO Implement scriptLabel2.

    init {
        require(data.size == OBJECT_BYTE_SIZE) {
            "Data size should be $OBJECT_BYTE_SIZE but was ${data.size}."
        }
    }
}
