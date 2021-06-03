package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.ninja.angleToRad
import world.phantasmal.lib.fileFormats.ninja.radToAngle
import kotlin.math.roundToInt

class QuestObject(override var areaId: Int, override val data: Buffer) : QuestEntity<ObjectType> {
    constructor(type: ObjectType, areaId: Int) : this(areaId, Buffer.withSize(OBJECT_BYTE_SIZE)) {
        setObjectDefaultData(type, data)
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

    var id: Short
        get() = data.getShort(8)
        set(value) {
            data.setShort(8, value)
        }

    var groupId: Short
        get() = data.getShort(10)
        set(value) {
            data.setShort(10, value)
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

    /**
     * The offset of the model property or -1 if this object doesn't have a model property.
     */
    val modelOffset: Int
        get() = when (type) {
            ObjectType.Probe,
            -> 40

            ObjectType.Saw,
            ObjectType.LaserDetect,
            -> 48

            ObjectType.Sonic,
            ObjectType.LittleCryotube,
            ObjectType.Cactus,
            ObjectType.BigBrownRock,
            ObjectType.BigBlackRocks,
            ObjectType.BeeHive,
            -> 52

            ObjectType.ForestConsole,
            -> 56

            ObjectType.PrincipalWarp,
            ObjectType.LaserFence,
            ObjectType.LaserSquareFence,
            ObjectType.LaserFenceEx,
            ObjectType.LaserSquareFenceEx,
            -> 60

            else -> -1
        }

    var model: Int
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

            else -> throw IllegalArgumentException("$type doesn't have a model property.")
        }
        set(value) {
            when (type) {
                ObjectType.Probe,
                -> data.setFloat(40, value.toFloat())

                ObjectType.Saw,
                ObjectType.LaserDetect,
                -> data.setFloat(48, value.toFloat())

                ObjectType.Sonic,
                ObjectType.LittleCryotube,
                ObjectType.Cactus,
                ObjectType.BigBrownRock,
                ObjectType.BigBlackRocks,
                ObjectType.BeeHive,
                -> data.setInt(52, value)

                ObjectType.ForestConsole,
                -> data.setInt(56, value)

                ObjectType.PrincipalWarp,
                ObjectType.LaserFence,
                ObjectType.LaserSquareFence,
                ObjectType.LaserFenceEx,
                ObjectType.LaserSquareFenceEx,
                -> data.setInt(60, value)

                else -> throw IllegalArgumentException("$type doesn't have a model property.")
            }
        }

    val destinationPositionOffset: Int
        get() = when (type) {
            ObjectType.Warp, ObjectType.PrincipalWarp, ObjectType.RuinsWarpSiteToSite -> 40
            else -> -1
        }

    /**
     * Only valid for [ObjectType.Warp], [ObjectType.PrincipalWarp] and
     * [ObjectType.RuinsWarpSiteToSite].
     */
    var destinationPosition: Vec3
        get() = Vec3(
            data.getFloat(40),
            data.getFloat(44),
            data.getFloat(48),
        )
        set(value) {
            setDestinationPosition(value.x, value.y, value.z)
        }

    /**
     * Only valid for [ObjectType.Warp], [ObjectType.PrincipalWarp] and
     * [ObjectType.RuinsWarpSiteToSite].
     */
    var destinationPositionX: Float
        get() = data.getFloat(40)
        set(value) {
            data.setFloat(40, value)
        }

    /**
     * Only valid for [ObjectType.Warp], [ObjectType.PrincipalWarp] and
     * [ObjectType.RuinsWarpSiteToSite].
     */
    var destinationPositionY: Float
        get() = data.getFloat(44)
        set(value) {
            data.setFloat(44, value)
        }

    /**
     * Only valid for [ObjectType.Warp], [ObjectType.PrincipalWarp] and
     * [ObjectType.RuinsWarpSiteToSite].
     */
    var destinationPositionZ: Float
        get() = data.getFloat(48)
        set(value) {
            data.setFloat(48, value)
        }

    val destinationRotationYOffset: Int
        get() = when (type) {
            ObjectType.Warp, ObjectType.PrincipalWarp, ObjectType.RuinsWarpSiteToSite -> 52
            else -> -1
        }

    /**
     * Only valid for [ObjectType.Warp], [ObjectType.PrincipalWarp] and
     * [ObjectType.RuinsWarpSiteToSite].
     */
    var destinationRotationY: Float
        get() = angleToRad(data.getInt(52))
        set(value) {
            data.setInt(52, radToAngle(value))
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

    fun setDestinationPosition(x: Float, y: Float, z: Float) {
        data.setFloat(40, x)
        data.setFloat(44, y)
        data.setFloat(48, z)
    }
}
