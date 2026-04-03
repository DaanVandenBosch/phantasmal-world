package world.phantasmal.web.questEditor.loading

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.quest.*

// DAT table header constants are defined in psolib's Dat.kt (DAT_HEADER_SIZE,
// DAT_ENTITY_TYPE_OBJ, DAT_ENTITY_TYPE_NPC, DAT_ENTITY_TYPE_EVENT).

/**
 * A floor's raw entity data for synthesizing into standard DAT format.
 */
class DatFloorSection(
    val floorId: Int,
    val objData: Buffer,
    val npcData: Buffer,
    /** Raw .evt file content (EventsSectionHeader + Event1Entry[] + action stream). */
    val eventData: Buffer = Buffer.withSize(0, Endianness.Little),
)

/**
 * Extract raw entity data from a quest, split by floor (areaId).
 * Returns a map of floorId → (objBuffer, npcBuffer) for writing back to per-floor dat files.
 */
fun extractRawEntityDataByFloor(quest: Quest): Map<Int, Pair<Buffer, Buffer>> {
    val objsByFloor = quest.objects.groupBy { it.areaId }
    val npcsByFloor = quest.npcs.groupBy { it.areaId }
    val allFloorIds = (objsByFloor.keys + npcsByFloor.keys).sorted()

    return allFloorIds.associateWith { floorId ->
        val floorObjs = objsByFloor[floorId] ?: emptyList()
        val floorNpcs = npcsByFloor[floorId] ?: emptyList()

        val objBuf = Buffer.withSize(floorObjs.size * OBJECT_BYTE_SIZE, Endianness.Little)
        var offset = 0
        for (obj in floorObjs) {
            obj.data.copyInto(objBuf, destinationOffset = offset)
            offset += OBJECT_BYTE_SIZE
        }

        val npcBuf = Buffer.withSize(floorNpcs.size * NPC_BYTE_SIZE, Endianness.Little)
        offset = 0
        for (npc in floorNpcs) {
            npc.data.copyInto(npcBuf, destinationOffset = offset)
            offset += NPC_BYTE_SIZE
        }

        Pair(objBuf, npcBuf)
    }
}

/**
 * Synthesize a standard DAT file from multiple floors' raw entity data.
 */
fun synthesizeDat(sections: List<DatFloorSection>): Cursor {
    val totalSize = sections.sumOf { section ->
        val objSize = section.objData.size
        val npcSize = section.npcData.size
        val evtSize = section.eventData.size
        (if (objSize > 0) DAT_HEADER_SIZE + objSize else 0) +
            (if (npcSize > 0) DAT_HEADER_SIZE + npcSize else 0) +
            (if (evtSize > 0) DAT_HEADER_SIZE + evtSize else 0)
    } + DAT_HEADER_SIZE // terminator

    val buf = Buffer.withSize(totalSize, Endianness.Little)
    var offset = 0

    fun writeDatSection(entityType: Int, floorId: Int, data: Buffer) {
        val size = data.size
        if (size > 0) {
            buf.setInt(offset, entityType)
            buf.setInt(offset + 4, DAT_HEADER_SIZE + size)
            buf.setInt(offset + 8, floorId)
            buf.setInt(offset + 12, size)
            offset += DAT_HEADER_SIZE
            data.copyInto(buf, destinationOffset = offset)
            offset += size
        }
    }

    for (section in sections) {
        writeDatSection(DAT_ENTITY_TYPE_OBJ, section.floorId, section.objData)
        writeDatSection(DAT_ENTITY_TYPE_NPC, section.floorId, section.npcData)
        writeDatSection(DAT_ENTITY_TYPE_EVENT, section.floorId, section.eventData)
    }

    // Terminator: 16 zero bytes (already zero from Buffer.withSize).

    return buf.cursor()
}
