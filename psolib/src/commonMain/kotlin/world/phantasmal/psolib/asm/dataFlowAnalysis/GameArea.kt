package world.phantasmal.psolib.asm.dataFlowAnalysis

/**
 * Represents all game areas with their mapping relationships between mapId, areaId, and episode.
 * This enum serves as the single source of truth for area mappings, ensuring consistency
 * between getAreaIdByMapId and getMapId functions.
 */
enum class GameArea(
    val mapId: Int,
    val areaId: Int,
    val episode: Int,
    val displayName: String
) {
    // Episode I
    PIONEER2_EP1(0x00, 0, 0, "Pioneer2_Ep1"),
    FOREST1(0x01, 1, 0, "Forest1"),
    FOREST2(0x02, 2, 0, "Forest2"),
    CAVE1(0x03, 3, 0, "Cave1"),
    CAVE2(0x04, 4, 0, "Cave2"),
    CAVE3(0x05, 5, 0, "Cave3"),
    MINES1(0x06, 6, 0, "Mines1"),
    MINES2(0x07, 7, 0, "Mines2"),
    RUINS1(0x08, 8, 0, "Ruins1"),
    RUINS2(0x09, 9, 0, "Ruins2"),
    RUINS3(0x0A, 10, 0, "Ruins3"),
    BOSS_DRAGON(0x0B, 11, 0, "Boss_Dragon"),
    BOSS_DEROLLE(0x0C, 12, 0, "Boss_Derolle"),
    BOSS_VOLOPT(0x0D, 13, 0, "Boss_Volopt"),
    BOSS_DARKFALZ(0x0E, 14, 0, "Boss_Darkfalz"),
    LOBBY(0x0F, 15, 0, "Lobby"),
    BATTLE_SPACESHIP(0x10, 16, 0, "Battle_Spaceship"),
    BATTLE_RUINS(0x11, 17, 0, "Battle_Ruins"),

    // Episode II
    PIONEER2_EP2(0x12, 0, 1, "Pioneer2_Ep2"),
    TEMPLE_A(0x13, 1, 1, "Temple_A"),
    TEMPLE_B(0x14, 2, 1, "Temple_B"),
    SPACESHIP_A(0x15, 3, 1, "Spaceship_A"),
    SPACESHIP_B(0x16, 4, 1, "Spaceship_B"),
    CCA(0x17, 5, 1, "CCA"),
    JUNGLE_EAST(0x18, 6, 1, "Jungle_East"),
    JUNGLE_NORTH(0x19, 7, 1, "Jungle_North"),
    MOUNTAIN(0x1A, 8, 1, "Mountain"),
    SEASIDE(0x1B, 9, 1, "Seaside"),
    SEABED_UPPER(0x1C, 10, 1, "Seabed_Upper"),
    SEABED_LOWER(0x1D, 11, 1, "Seabed_Lower"),
    BOSS_GALGRYPHON(0x1E, 12, 1, "Boss_Galgryphon"),
    BOSS_OLGAFLOW(0x1F, 13, 1, "Boss_Olgaflow"),
    BOSS_BARBARAY(0x20, 14, 1, "Boss_Barbaray"),
    BOS_GOLDRAGON(0x21, 15, 1, "Bos_GolDragon"),
    SEASIDE_NIGHT(0x22, 16, 1, "Seaside_Night"),
    TOWER(0x23, 17, 1, "Tower"),

    // Episode IV
    WILDS1(0x24, 1, 2, "Wilds1"),
    WILDS2(0x25, 2, 2, "Wilds2"),
    WILDS3(0x26, 3, 2, "Wilds3"),
    WILDS4(0x27, 4, 2, "Wilds4"),
    CRATER(0x28, 5, 2, "Crater"),
    DESERT1(0x29, 6, 2, "Desert1"),
    DESERT2(0x2A, 7, 2, "Desert2"),
    DESERT3(0x2B, 8, 2, "Desert3"),
    BOSS_SAINTMILION(0x2C, 9, 2, "Boss_SaintMilion"),
    PIONEER2_EP4(0x2D, 0, 2, "Pioneer2_Ep4");

    companion object {
        private val mapIdToArea: Map<Int, GameArea> by lazy {
            entries.associateBy { it.mapId }
        }

        private val episodeAreaToArea: Map<Pair<Int, Int>, GameArea> by lazy {
            entries.associateBy { Pair(it.episode, it.areaId) }
        }

        /**
         * Finds a GameArea by its mapId.
         * @param mapId The game-internal map ID
         * @return The corresponding GameArea or null if not found
         */
        fun findByMapId(mapId: Int): GameArea? = mapIdToArea[mapId]

        /**
         * Finds a GameArea by episode and areaId.
         * @param episode The episode number (0=I, 1=II, 2=IV)
         * @param areaId The area ID within the episode
         * @return The corresponding GameArea or null if not found
         */
        fun findByEpisodeAndArea(episode: Int, areaId: Int): GameArea? =
            episodeAreaToArea[Pair(episode, areaId)]

        /**
         * Gets all areas for a specific episode.
         * @param episode The episode number (0=I, 1=II, 2=IV)
         * @return List of all GameAreas in that episode
         */
        fun getAreasForEpisode(episode: Int): List<GameArea> =
            entries.filter { it.episode == episode }
    }
}