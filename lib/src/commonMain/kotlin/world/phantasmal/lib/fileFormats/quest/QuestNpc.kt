package world.phantasmal.lib.fileFormats.quest

import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.fileFormats.Vec3
import world.phantasmal.lib.fileFormats.ninja.angleToRad
import world.phantasmal.lib.fileFormats.ninja.radToAngle
import kotlin.math.roundToInt

class QuestNpc(
    var episode: Episode,
    override var areaId: Int,
    val data: Buffer,
) : QuestEntity<NpcType> {
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

    override var position: Vec3
        get() = Vec3(data.getFloat(20), data.getFloat(24), data.getFloat(28))
        set(value) {
            data.setFloat(20, value.x)
            data.setFloat(24, value.y)
            data.setFloat(28, value.z)
        }

    override var rotation: Vec3
        get() = Vec3(
            angleToRad(data.getInt(32)),
            angleToRad(data.getInt(36)),
            angleToRad(data.getInt(40)),
        )
        set(value) {
            data.setInt(32, radToAngle(value.x))
            data.setInt(36, radToAngle(value.y))
            data.setInt(40, radToAngle(value.z))
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
}
