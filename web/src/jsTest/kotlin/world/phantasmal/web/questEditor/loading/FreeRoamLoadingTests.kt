package world.phantasmal.web.questEditor.loading

import world.phantasmal.psolib.Episode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FreeRoamLoadingTests {

    // --- Bin filename parsing ---

    @Test
    fun parse_ep1_city_bin() {
        val info = parseFreeRoamFilename("map_city_on_j.bin")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(true, info.isCity)
        assertEquals(false, info.offline)
        assertEquals(false, info.ultimate)
        assertEquals("map_city", info.binPrefix)
        assertEquals(0..0, info.floorRange)
    }

    @Test
    fun parse_ep1_city_offline_bin() {
        val info = parseFreeRoamFilename("map_city_off_e.bin")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(true, info.isCity)
        assertEquals(true, info.offline)
    }

    @Test
    fun parse_ep2_lab_bin() {
        val info = parseFreeRoamFilename("map_labo_on_j.bin")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(true, info.isCity)
        assertEquals("map_labo", info.binPrefix)
    }

    @Test
    fun parse_ep4_city_bin() {
        val info = parseFreeRoamFilename("map_city02_on_e.bin")
        assertNotNull(info)
        assertEquals(Episode.IV, info.episode)
        assertEquals(true, info.isCity)
        assertEquals("map_city02", info.binPrefix)
    }

    @Test
    fun parse_ep2_field_bin() {
        val info = parseFreeRoamFilename("map_ruin_j.bin")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(false, info.isCity)
        assertEquals("map_ruin", info.binPrefix)
        assertEquals("ruins", info.tokenBase)
        assertEquals(false, info.ultimate)
    }

    @Test
    fun parse_ep2_field_ultimate_bin() {
        val info = parseFreeRoamFilename("map_seabed_j_u.bin")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals("map_seabed", info.binPrefix)
        assertEquals(true, info.ultimate)
    }

    @Test
    fun parse_ep2_jungle_bin() {
        val info = parseFreeRoamFilename("map_jungle_j.bin")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals("map_jungle", info.binPrefix)
        assertEquals(5..9, info.floorRange)
    }

    @Test
    fun parse_unknown_bin_returns_null() {
        assertNull(parseFreeRoamFilename("quest_01.bin"))
        assertNull(parseFreeRoamFilename("map_unknown_j.bin"))
    }

    // --- Dat filename parsing ---

    @Test
    fun parse_ep1_forest_dat() {
        val info = parseFreeRoamFilename("map_forest01_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(1..2, info.floorRange)
        assertEquals("forest", info.tokenBase)
        assertEquals("map_forest", info.binPrefix)
    }

    @Test
    fun parse_ep1_cave_dat() {
        val info = parseFreeRoamFilename("map_cave02_01_00e.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(3..5, info.floorRange)
    }

    @Test
    fun parse_ep1_machine_dat() {
        val info = parseFreeRoamFilename("map_machine01_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(6..7, info.floorRange)
    }

    @Test
    fun parse_ep1_ancient_dat() {
        val info = parseFreeRoamFilename("map_ancient03_02_01e.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(8..10, info.floorRange)
    }

    @Test
    fun parse_ep2_ruins_dat() {
        val info = parseFreeRoamFilename("map_ruins01_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(1..2, info.floorRange)
    }

    @Test
    fun parse_ep2_jungle_dat() {
        val info = parseFreeRoamFilename("map_jungle03_02e.dat")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(5..9, info.floorRange)
    }

    @Test
    fun parse_ep4_wilds_dat() {
        val info = parseFreeRoamFilename("map_wilds01_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.IV, info.episode)
        assertEquals(1..4, info.floorRange)
    }

    @Test
    fun parse_ep4_crater_dat() {
        val info = parseFreeRoamFilename("map_crater01_00_00e.dat")
        assertNotNull(info)
        assertEquals(Episode.IV, info.episode)
        assertEquals(5..5, info.floorRange)
    }

    @Test
    fun parse_ep4_desert_dat() {
        val info = parseFreeRoamFilename("map_desert02_00_01o.dat")
        assertNotNull(info)
        assertEquals(Episode.IV, info.episode)
        assertEquals(6..8, info.floorRange)
    }

    @Test
    fun parse_ep1_city_dat() {
        val info = parseFreeRoamFilename("map_city00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(true, info.isCity)
        assertEquals(0..0, info.floorRange)
    }

    @Test
    fun parse_ep4_city_dat() {
        val info = parseFreeRoamFilename("map_city02_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.IV, info.episode)
        assertEquals(true, info.isCity)
        assertEquals(0..0, info.floorRange)
    }

    @Test
    fun parse_offline_city_dat() {
        val info = parseFreeRoamFilename("map_city00_00o_s.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(true, info.isCity)
    }

    @Test
    fun parse_unknown_dat_returns_null() {
        assertNull(parseFreeRoamFilename("quest_01.dat"))
        assertNull(parseFreeRoamFilename("not_a_dat.txt"))
    }

    @Test
    fun parse_ep2_seabed_dat() {
        val info = parseFreeRoamFilename("map_seabed01_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(10..11, info.floorRange)
    }

    @Test
    fun parse_ep2_space_dat() {
        val info = parseFreeRoamFilename("map_space01_00_00e.dat")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(3..4, info.floorRange)
    }
}
