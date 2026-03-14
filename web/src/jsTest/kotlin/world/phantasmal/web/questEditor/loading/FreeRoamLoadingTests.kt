package world.phantasmal.web.questEditor.loading

import world.phantasmal.psolib.Episode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FreeRoamLoadingTests {

    // --- parseFreeRoamBinFilename tests ---

    @Test
    fun parse_ep1_city_bin() {
        val info = parseFreeRoamBinFilename("map_city_on_j.bin")
        assertNotNull(info)
        assertIs<FreeRoamBinInfo.City>(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(false, info.offline)
        assertEquals(false, info.ultimate)
        assertEquals("map_city", info.binPrefix)
    }

    @Test
    fun parse_ep1_city_offline_bin() {
        val info = parseFreeRoamBinFilename("map_city_off_e.bin")
        assertNotNull(info)
        info as FreeRoamBinInfo.City
        assertEquals(Episode.I, info.episode)
        assertEquals(true, info.offline)
    }

    @Test
    fun parse_ep2_lab_bin() {
        val info = parseFreeRoamBinFilename("map_labo_on_j.bin")
        assertNotNull(info)
        info as FreeRoamBinInfo.City
        assertEquals(Episode.II, info.episode)
        assertEquals("map_labo", info.binPrefix)
    }

    @Test
    fun parse_ep4_city_bin() {
        val info = parseFreeRoamBinFilename("map_city02_on_e.bin")
        assertNotNull(info)
        info as FreeRoamBinInfo.City
        assertEquals(Episode.IV, info.episode)
        assertEquals("map_city02", info.binPrefix)
    }

    @Test
    fun parse_ep2_field_bin() {
        val info = parseFreeRoamBinFilename("map_ruin_j.bin")
        assertNotNull(info)
        info as FreeRoamBinInfo.Field
        assertEquals(Episode.II, info.episode)
        assertEquals("map_ruin", info.prefix)
        assertEquals(false, info.ultimate)
    }

    @Test
    fun parse_ep2_field_ultimate_bin() {
        val info = parseFreeRoamBinFilename("map_seabed_j_u.bin")
        assertNotNull(info)
        info as FreeRoamBinInfo.Field
        assertEquals(Episode.II, info.episode)
        assertEquals("map_seabed", info.prefix)
        assertEquals(true, info.ultimate)
    }

    @Test
    fun parse_ep2_jungle_bin() {
        val info = parseFreeRoamBinFilename("map_jungle_j.bin")
        assertNotNull(info)
        info as FreeRoamBinInfo.Field
        assertEquals(Episode.II, info.episode)
        assertEquals("map_jungle", info.prefix)
    }

    @Test
    fun parse_unknown_bin_returns_null() {
        assertNull(parseFreeRoamBinFilename("quest_01.bin"))
        assertNull(parseFreeRoamBinFilename("map_unknown_j.bin"))
        assertNull(parseFreeRoamBinFilename("not_a_bin.dat"))
    }

    // --- parseFreeRoamDatFilename tests ---

    @Test
    fun parse_ep1_forest_dat() {
        val info = parseFreeRoamDatFilename("map_forest01_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(1..2, info.floorRange)
    }

    @Test
    fun parse_ep1_cave_dat() {
        val info = parseFreeRoamDatFilename("map_cave02_01_00e.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(3..5, info.floorRange)
    }

    @Test
    fun parse_ep1_machine_dat() {
        val info = parseFreeRoamDatFilename("map_machine01_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(6..7, info.floorRange)
    }

    @Test
    fun parse_ep1_ancient_dat() {
        val info = parseFreeRoamDatFilename("map_ancient03_02_01e.dat")
        assertNotNull(info)
        assertEquals(Episode.I, info.episode)
        assertEquals(8..10, info.floorRange)
    }

    @Test
    fun parse_ep2_ruins_dat() {
        val info = parseFreeRoamDatFilename("map_ruins01_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(1..2, info.floorRange)
    }

    @Test
    fun parse_ep2_jungle_dat() {
        val info = parseFreeRoamDatFilename("map_jungle03_02e.dat")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(5..9, info.floorRange)
    }

    @Test
    fun parse_ep4_wilds_dat() {
        val info = parseFreeRoamDatFilename("map_wilds01_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.IV, info.episode)
        assertEquals(1..4, info.floorRange)
    }

    @Test
    fun parse_ep4_crater_dat() {
        val info = parseFreeRoamDatFilename("map_crater01_00_00e.dat")
        assertNotNull(info)
        assertEquals(Episode.IV, info.episode)
        assertEquals(5..5, info.floorRange)
    }

    @Test
    fun parse_ep4_desert_dat() {
        val info = parseFreeRoamDatFilename("map_desert02_00_01o.dat")
        assertNotNull(info)
        assertEquals(Episode.IV, info.episode)
        assertEquals(6..8, info.floorRange)
    }

    @Test
    fun parse_offline_dat() {
        // Offline city dat files have _s suffix — these are city files, not field
        val info = parseFreeRoamDatFilename("map_city00_00o_s.dat")
        // city is not in DAT_TOKEN_BASE_TO_AREA_GROUP, so should return null
        assertNull(info)
    }

    @Test
    fun parse_unknown_dat_returns_null() {
        assertNull(parseFreeRoamDatFilename("quest_01.dat"))
        assertNull(parseFreeRoamDatFilename("not_a_dat.bin"))
        assertNull(parseFreeRoamDatFilename("map_city00_00o.dat"))
    }

    @Test
    fun parse_ep2_seabed_dat() {
        val info = parseFreeRoamDatFilename("map_seabed01_00_00o.dat")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(10..11, info.floorRange)
    }

    @Test
    fun parse_ep2_space_dat() {
        val info = parseFreeRoamDatFilename("map_space01_00_00e.dat")
        assertNotNull(info)
        assertEquals(Episode.II, info.episode)
        assertEquals(3..4, info.floorRange)
    }
}
