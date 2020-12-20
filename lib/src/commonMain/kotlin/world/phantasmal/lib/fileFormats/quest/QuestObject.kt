package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.ninja.angleToRad
import world.phantasmal.lib.fileFormats.ninja.radToAngle
import kotlin.math.roundToInt

class QuestObject(override var areaId: Int, val data: Buffer) : QuestEntity<ObjectType> {
    constructor(type: ObjectType, areaId: Int) : this(areaId, Buffer.withSize(OBJECT_BYTE_SIZE)) {
        // TODO: Set default data.
        this.type = type
    }

    var typeId: Short
        get() = data.getShort(0)
        set(value) {
            data.setShort(0, value)
        }

    override var type: ObjectType
        get() = objectTypeFromId(typeId)
        set(value) {
            typeId = value.typeId ?: -1
        }

    override var sectionId: Short
        get() = data.getShort(12)
        set(value) {
            data.setShort(12, value)
        }

    override var position: Vec3
        get() = Vec3(data.getFloat(16), data.getFloat(20), data.getFloat(24))
        set(value) {
            setPosition(value.x, value.y, value.z)
        }

    override var rotation: Vec3
        get() = Vec3(
            angleToRad(data.getInt(28)),
            angleToRad(data.getInt(32)),
            angleToRad(data.getInt(36)),
        )
        set(value) {
            setRotation(value.x, value.y, value.z)
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

    override fun setPosition(x: Float, y: Float, z: Float) {
        data.setFloat(16, x)
        data.setFloat(20, y)
        data.setFloat(24, z)
    }

    override fun setRotation(x: Float, y: Float, z: Float) {
        data.setInt(28, radToAngle(x))
        data.setInt(32, radToAngle(y))
        data.setInt(36, radToAngle(z))
    }
}
