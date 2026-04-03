package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.fileFormats.Vec3
import world.phantasmal.psolib.fileFormats.ninja.angleToRad
import world.phantasmal.psolib.fileFormats.ninja.radToAngle
import kotlin.math.roundToInt

class QuestNpc(
    var episode: Episode,
    override var areaId: Int,
    override val data: Buffer,
) : QuestEntity<NpcType> {
    /**
     * Game area ID mapped from floor mapping (e.g., 17 for Tower).
     * Defaults to areaId if no floor mapping exists.
     * Used for NPC type detection while areaId is used for floor/variant mapping.
     */
    var gameAreaId: Int = areaId
    constructor(
        type: NpcType,
        episode: Episode,
        areaId: Int,
        wave: Short,
    ) : this(episode, areaId, Buffer.withSize(NPC_BYTE_SIZE)) {
        setNpcDefaultData(type, data)
        this.type = type
        // Set areaId after type, because you might want to overwrite the areaId that type has
        // determined.
        this.areaId = areaId
        this.wave = wave
        this.wave2 = wave.toInt()
    }

    var typeId: Short
        get() = data.getShort(0)
        set(value) {
            data.setShort(0, value)
        }

    override var type: NpcType
        get() = npcTypeFromQuestNpc(this)
        set(value) {
            value.episode?.let { episode = it }
            typeId = (value.typeId ?: 0).toShort()

            when (value) {
                NpcType.SaintMilion,
                NpcType.SavageWolf,
                NpcType.BarbarousWolf,
                NpcType.PoisonLily,
                NpcType.NarLily,
                NpcType.PofuillySlime,
                NpcType.PouillySlime,
                NpcType.PoisonLily2,
                NpcType.NarLily2,
                NpcType.SavageWolf2,
                NpcType.BarbarousWolf2,
                NpcType.Kondrieu,
                NpcType.Shambertin,
                NpcType.SinowBeat,
                NpcType.SinowGold,
                NpcType.SatelliteLizard,
                NpcType.Yowie,
                -> special = value.special ?: false

                else -> {
                    // Do nothing.
                }
            }

            skin = value.skin ?: 0

            if (value.areaIds.isNotEmpty() && areaId !in value.areaIds) {
                areaId = value.areaIds.first()
            }
        }

    override var sectionId: Short
        get() = data.getShort(12)
        set(value) {
            data.setShort(12, value)
        }

    var wave: Short
        get() = data.getShort(14)
        set(value) {
            data.setShort(14, value)
        }

    var wave2: Int
        get() = data.getInt(16)
        set(value) {
            data.setInt(16, value)
        }

    override var position: Vec3
        get() = Vec3(data.getFloat(20), data.getFloat(24), data.getFloat(28))
        set(value) {
            setPosition(value.x, value.y, value.z)
        }

    override var rotation: Vec3
        get() = Vec3(
            angleToRad(data.getInt(32)),
            angleToRad(data.getInt(36)),
            angleToRad(data.getInt(40)),
        )
        set(value) {
            setRotation(value.x, value.y, value.z)
        }

    /**
     * Only seems to be valid for non-enemies.
     */
    var id: Int
        get() = data.getFloat(56).roundToInt()
        set(value) {
            data.setFloat(56, value.toFloat())
        }

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

    var special: Boolean
        get() = data.getFloat(48).roundToInt() == 1
        set(value) {
            data.setFloat(48, if (value) 1f else 0f)
        }

    init {
        require(data.size == NPC_BYTE_SIZE) {
            "Data size should be $NPC_BYTE_SIZE but was ${data.size}."
        }
    }

    override fun setPosition(x: Float, y: Float, z: Float) {
        data.setFloat(20, x)
        data.setFloat(24, y)
        data.setFloat(28, z)
    }

    override fun setRotation(x: Float, y: Float, z: Float) {
        data.setInt(32, radToAngle(x))
        data.setInt(36, radToAngle(y))
        data.setInt(40, radToAngle(z))
    }
}
