package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.ninja.angleToRad
import world.phantasmal.lib.fileFormats.ninja.radToAngle
import kotlin.math.roundToInt

class QuestObject(override var areaId: Int, val data: Buffer) : QuestEntity<ObjectType> {
    var typeId: Int
        get() = data.getInt(0)
        set(value) {
            data.setInt(0, value)
        }

    override var type: ObjectType
        get() = objectTypeFromId(typeId)
        set(value) {
            typeId = value.typeId ?: -1
        }

    override var sectionId: Int
        get() = data.getUShort(12).toInt()
        set(value) {
            data.setUShort(12, value.toUShort())
        }

    override var position: Vec3
        get() = Vec3(data.getFloat(16), data.getFloat(20), data.getFloat(24))
        set(value) {
            data.setFloat(16, value.x)
            data.setFloat(20, value.y)
            data.setFloat(24, value.z)
        }

    override var rotation: Vec3
        get() = Vec3(
            angleToRad(data.getInt(28)),
            angleToRad(data.getInt(32)),
            angleToRad(data.getInt(36)),
        )
        set(value) {
            data.setInt(28, radToAngle(value.x))
            data.setInt(32, radToAngle(value.y))
            data.setInt(36, radToAngle(value.z))
        }

    val scriptLabel: Int?
        get() = when (type) {
            ObjectType.ScriptCollision,
            ObjectType.ForestConsole,
            ObjectType.TalkLinkToSupport,
            -> data.getInt(52)

            ObjectType.RicoMessagePod,
            -> data.getInt(56)

            else -> null
        }

    val scriptLabel2: Int?
        get() = if (type == ObjectType.RicoMessagePod) data.getInt(60) else null

    val model: Int?
        get() = when (type) {
            ObjectType.Probe,
            -> data.getFloat(40).roundToInt()

            ObjectType.Saw,
            ObjectType.LaserDetect,
            -> data.getFloat(48).roundToInt()

            ObjectType.Sonic,
            ObjectType.LittleCryotube,
            ObjectType.Cactus,
            ObjectType.BigBrownRock,
            ObjectType.BigBlackRocks,
            ObjectType.BeeHive,
            -> data.getInt(52)

            ObjectType.ForestConsole,
            -> data.getInt(56)

            ObjectType.PrincipalWarp,
            ObjectType.LaserFence,
            ObjectType.LaserSquareFence,
            ObjectType.LaserFenceEx,
            ObjectType.LaserSquareFenceEx,
            -> data.getInt(60)

            else -> null
        }

    init {
        require(data.size == OBJECT_BYTE_SIZE) {
            "Data size should be $OBJECT_BYTE_SIZE but was ${data.size}."
        }
    }
}
