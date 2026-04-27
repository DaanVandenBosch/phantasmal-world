package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Episode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SetDataTableTests {

    @Test
    fun resolve_city_dat_filenames() {
        val info = getFloorFileInfo(Episode.I, 0)
        assertNotNull(info)
        assertEquals("city00", info.token)

        assertEquals("map_city00_00o.dat", resolveDatFilename(info, 0, 0, DatFileType.OBJECTS))
        assertEquals("map_city00_00e.dat", resolveDatFilename(info, 0, 0, DatFileType.ENEMIES))
    }

    @Test
    fun resolve_forest_dat_filenames() {
        val info = getFloorFileInfo(Episode.I, 1)
        assertNotNull(info)
        assertEquals("forest01", info.token)
        assertEquals(0, info.v1Values.size)
        assertEquals(5, info.v2Values.size)

        // No v1 dimension, only v2
        assertEquals("map_forest01_00o.dat", resolveDatFilename(info, 0, 0, DatFileType.OBJECTS))
        assertEquals("map_forest01_04e.dat", resolveDatFilename(info, 0, 4, DatFileType.ENEMIES))
    }

    @Test
    fun resolve_cave_dat_filenames() {
        val info = getFloorFileInfo(Episode.I, 3)
        assertNotNull(info)
        assertEquals("cave01", info.token)
        assertEquals(3, info.v1Values.size)
        assertEquals(2, info.v2Values.size)

        // Both v1 and v2 dimensions
        assertEquals("map_cave01_00_00o.dat", resolveDatFilename(info, 0, 0, DatFileType.OBJECTS))
        assertEquals("map_cave01_02_01e.dat", resolveDatFilename(info, 2, 1, DatFileType.ENEMIES))
    }

    @Test
    fun resolve_ep2_vr_temple_dat_filenames() {
        val info = getFloorFileInfo(Episode.II, 1)
        assertNotNull(info)
        assertEquals("ruins01", info.token)

        assertEquals("map_ruins01_00_00o.dat", resolveDatFilename(info, 0, 0, DatFileType.OBJECTS))
        assertEquals("map_ruins01_01_00e.dat", resolveDatFilename(info, 1, 0, DatFileType.ENEMIES))
    }

    @Test
    fun resolve_ep2_cca_dat_filenames() {
        val info = getFloorFileInfo(Episode.II, 5)
        assertNotNull(info)
        assertEquals("jungle01", info.token)

        assertEquals("map_jungle01_00o.dat", resolveDatFilename(info, 0, 0, DatFileType.OBJECTS))
        assertEquals("map_jungle01_02e.dat", resolveDatFilename(info, 0, 2, DatFileType.ENEMIES))
    }

    @Test
    fun resolve_ep4_crater_route_dat_filenames() {
        // Crater Route 1-4 all use wilds01, distinguished by v1
        val info1 = getFloorFileInfo(Episode.IV, 1)
        assertNotNull(info1)
        assertEquals("wilds01", info1.token)
        assertEquals(listOf(0), info1.v1Values)

        val info2 = getFloorFileInfo(Episode.IV, 2)
        assertNotNull(info2)
        assertEquals("wilds01", info2.token)
        assertEquals(listOf(1), info2.v1Values)

        // Floor 1 default: v1=0 (from v1Values[0])
        assertEquals("map_wilds01_00_00o.dat", resolveDatFilename(info1, 0, 0, DatFileType.OBJECTS))
        // Floor 2 default: v1=1 (from v1Values[0])
        assertEquals("map_wilds01_01_00o.dat", resolveDatFilename(info2, 1, 0, DatFileType.OBJECTS))
    }

    @Test
    fun resolve_ep4_desert_dat_filenames() {
        val info = getFloorFileInfo(Episode.IV, 6)
        assertNotNull(info)
        assertEquals("desert01", info.token)
        assertEquals(3, info.v1Values.size)
        assertEquals(1, info.v2Values.size)

        assertEquals("map_desert01_00_00o.dat", resolveDatFilename(info, 0, 0, DatFileType.OBJECTS))
        assertEquals("map_desert01_02_00e.dat", resolveDatFilename(info, 2, 0, DatFileType.ENEMIES))
    }

    @Test
    fun resolve_ep4_event_filenames() {
        val info = getFloorFileInfo(Episode.IV, 1)
        assertNotNull(info)
        assertEquals("map_wilds01_00_00.evt", resolveDatFilename(info, 0, 0, DatFileType.EVENTS))
    }

    @Test
    fun resolve_ep4_city02_dat_filenames() {
        val info = getFloorFileInfo(Episode.IV, 0)
        assertNotNull(info)
        assertEquals("city02", info.token)
        assertEquals("map_city02_00_00o.dat", resolveDatFilename(info, 0, 0, DatFileType.OBJECTS))
        assertEquals("map_city02_00_00e.dat", resolveDatFilename(info, 0, 0, DatFileType.ENEMIES))
    }

    @Test
    fun boss_floors_return_null() {
        // EP1 boss floors
        assertNull(getFloorFileInfo(Episode.I, 11))
        assertNull(getFloorFileInfo(Episode.I, 14))

        // EP2 boss floors
        assertNull(getFloorFileInfo(Episode.II, 12))
        assertNull(getFloorFileInfo(Episode.II, 15))

        // EP4 boss floor
        assertNull(getFloorFileInfo(Episode.IV, 9))
    }

    @Test
    fun out_of_range_floors_return_null() {
        assertNull(getFloorFileInfo(Episode.I, 20))
        assertNull(getFloorFileInfo(Episode.IV, 15))
    }
}
