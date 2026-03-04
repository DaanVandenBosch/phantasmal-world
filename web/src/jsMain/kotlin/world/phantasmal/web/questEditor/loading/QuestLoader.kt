package world.phantasmal.web.questEditor.loading

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.quest.Quest
import world.phantasmal.psolib.fileFormats.quest.parseBinDatToQuest
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.webui.DisposableContainer

private class CitylabFiles(val bin: String, val npcDat: String, val objDat: String)

private val CITYLAB_FILES = mapOf(
    Episode.I to CitylabFiles(
        bin = "map_city_on_j.bin",
        npcDat = "map_city00_00e.dat",
        objDat = "map_city00_00o.dat",
    ),
    Episode.II to CitylabFiles(
        bin = "map_labo_on_j.bin",
        npcDat = "map_labo00_00e.dat",
        objDat = "map_labo00_00o.dat",
    ),
    Episode.IV to CitylabFiles(
        bin = "map_city02_on_j.bin",
        npcDat = "map_city02_00_00e.dat",
        objDat = "map_city02_00_00o.dat",
    ),
)

// DAT table header constants.
private const val DAT_HEADER_SIZE = 16
private const val DAT_ENTITY_TYPE_OBJ = 1
private const val DAT_ENTITY_TYPE_NPC = 2

class QuestLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    suspend fun loadDefaultQuest(episode: Episode): Quest {
        val files = CITYLAB_FILES[episode]
            ?: error("Episode $episode not supported.")

        val binBuf = assetLoader.loadArrayBuffer("/citylab/${files.bin}")
        val npcBuf = assetLoader.loadArrayBuffer("/citylab/${files.npcDat}")
        val objBuf = assetLoader.loadArrayBuffer("/citylab/${files.objDat}")

        val datCursor = synthesizeDat(
            objData = Buffer.fromArrayBuffer(objBuf, Endianness.Little),
            npcData = Buffer.fromArrayBuffer(npcBuf, Endianness.Little),
        )

        val questResult = parseBinDatToQuest(
            binCursor = binBuf.cursor(Endianness.Little),
            datCursor = datCursor,
            lenient = true,
            compressed = false,
        )

        val quest = questResult.unwrap()

        // Lobby .bin files may lack SET_EPISODE, so force the correct episode.
        quest.episode = episode

        for (npc in quest.npcs) {
            npc.episode = episode
        }

        return quest
    }
}

/**
 * Synthesize a standard DAT file from raw object and NPC entity arrays.
 * Format: [obj table header + obj data] [npc table header + npc data] [16-byte zero terminator]
 */
private fun synthesizeDat(objData: Buffer, npcData: Buffer): Cursor {
    val objSize = objData.size
    val npcSize = npcData.size
    val totalSize = DAT_HEADER_SIZE + objSize + DAT_HEADER_SIZE + npcSize + DAT_HEADER_SIZE

    val buf = Buffer.withSize(totalSize, Endianness.Little)
    var offset = 0

    // Object table header.
    buf.setInt(offset, DAT_ENTITY_TYPE_OBJ)
    buf.setInt(offset + 4, DAT_HEADER_SIZE + objSize)
    buf.setInt(offset + 8, 0) // areaId
    buf.setInt(offset + 12, objSize)
    offset += DAT_HEADER_SIZE

    // Object data.
    objData.copyInto(buf, destinationOffset = offset)
    offset += objSize

    // NPC table header.
    buf.setInt(offset, DAT_ENTITY_TYPE_NPC)
    buf.setInt(offset + 4, DAT_HEADER_SIZE + npcSize)
    buf.setInt(offset + 8, 0) // areaId
    buf.setInt(offset + 12, npcSize)
    offset += DAT_HEADER_SIZE

    // NPC data.
    npcData.copyInto(buf, destinationOffset = offset)
    offset += npcSize

    // Terminator: 16 zero bytes (already zero from Buffer.withSize).

    return buf.cursor()
}
