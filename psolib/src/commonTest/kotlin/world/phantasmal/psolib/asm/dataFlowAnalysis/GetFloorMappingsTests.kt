package world.phantasmal.psolib.asm.dataFlowAnalysis

import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.toInstructions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetFloorMappingsTests : LibTestSuite {
    @Test
    fun empty_segments_returns_empty_list() {
        val result = getFloorMappings(emptyList()) { error("should not be called") }
        assertTrue(result.isEmpty())
    }

    @Test
    fun bb_map_designate_extracts_floor_mappings() {
        val segments = toInstructions("""
            0:
                set_episode 1
                bb_map_designate 0, 18, 0, 0
                bb_map_designate 17, 35, 0, 0
                bb_map_designate 16, 35, 1, 0
                ret
        """.trimIndent())

        val result = getFloorMappings(segments) { ControlFlowGraph.create(segments) }

        assertEquals(3, result.size)

        val floor0 = result.find { it.floorId == 0 }!!
        assertEquals(18, floor0.mapId)
        assertEquals(0, floor0.areaId)
        assertEquals(0, floor0.variantId)
        assertEquals(Episode.II, floor0.mapEpisode)

        val floor17 = result.find { it.floorId == 17 }!!
        assertEquals(35, floor17.mapId)
        assertEquals(17, floor17.areaId)
        assertEquals(0, floor17.variantId)
        assertEquals(Episode.II, floor17.mapEpisode)

        val floor16 = result.find { it.floorId == 16 }!!
        assertEquals(35, floor16.mapId)
        assertEquals(17, floor16.areaId)
        assertEquals(1, floor16.variantId)
        assertEquals(Episode.II, floor16.mapEpisode)
    }

    @Test
    fun later_bb_map_designate_overwrites_earlier_for_same_floor() {
        val segments = toInstructions("""
            0:
                set_episode 1
                bb_map_designate 0, 18, 0, 0
                bb_map_designate 0, 35, 2, 0
                ret
        """.trimIndent())

        val result = getFloorMappings(segments) { ControlFlowGraph.create(segments) }

        // Only one entry for floor 0, and it should be the later one (mapId=35, Tower)
        assertEquals(1, result.size)
        val floor0 = result[0]
        assertEquals(0, floor0.floorId)
        assertEquals(35, floor0.mapId)
        assertEquals(17, floor0.areaId)
        assertEquals(2, floor0.variantId)
    }

    @Test
    fun set_floor_handler_before_bb_map_designate_does_not_win() {
        // set_floor_handler appears before bb_map_designate in label 0,
        // bb_map_designate overwrites it.
        val segments = toInstructions("""
            0:
                set_episode 1
                set_floor_handler 0, 300
                bb_map_designate 0, 35, 1, 0
                ret
            300:
                ret
        """.trimIndent())

        val result = getFloorMappings(segments) { ControlFlowGraph.create(segments) }

        val floor0 = result.find { it.floorId == 0 }!!
        // bb_map_designate should win: mapId=35 (Tower), not the default from set_floor_handler
        assertEquals(35, floor0.mapId)
        assertEquals(17, floor0.areaId)
        assertEquals(1, floor0.variantId)
    }

    @Test
    fun set_floor_handler_after_bb_map_designate_does_not_overwrite() {
        // bb_map_designate appears before set_floor_handler for the same floor,
        // set_floor_handler should NOT overwrite.
        val segments = toInstructions("""
            0:
                set_episode 1
                bb_map_designate 0, 35, 1, 0
                set_floor_handler 0, 300
                ret
            300:
                ret
        """.trimIndent())

        val result = getFloorMappings(segments) { ControlFlowGraph.create(segments) }

        val floor0 = result.find { it.floorId == 0 }!!
        assertEquals(35, floor0.mapId)
        assertEquals(17, floor0.areaId)
        assertEquals(1, floor0.variantId)
    }

    @Test
    fun set_floor_handler_fills_unmapped_floors() {
        // Floor 0 has both set_floor_handler and bb_map_designate -> bb_map_designate wins.
        // Floor 17 only has set_floor_handler -> should be filled in.
        val segments = toInstructions("""
            0:
                set_episode 1
                set_floor_handler 0, 300
                set_floor_handler 17, 310
                bb_map_designate 0, 18, 0, 0
                ret
            300:
                ret
            310:
                ret
        """.trimIndent())

        val result = getFloorMappings(segments) { ControlFlowGraph.create(segments) }

        // Floor 0: bb_map_designate wins
        val floor0 = result.find { it.floorId == 0 }!!
        assertEquals(18, floor0.mapId)
        assertEquals(0, floor0.areaId)

        // Floor 17: set_floor_handler fills in (areaId defaults to floorId)
        val floor17 = result.find { it.floorId == 17 }!!
        assertEquals(17, floor17.areaId)
        assertEquals(0, floor17.variantId)
        // set_floor_handler derives mapId from episode+floorId, and mapEpisode from that mapId
        assertTrue(floor17.mapId > 0)
        assertNotNull(floor17.mapEpisode)
    }

    @Test
    fun returns_flat_list_not_grouped() {
        val segments = toInstructions("""
            0:
                set_episode 1
                bb_map_designate 0, 18, 0, 0
                bb_map_designate 17, 35, 0, 0
                bb_map_designate 16, 35, 1, 0
                ret
        """.trimIndent())

        val result = getFloorMappings(segments) { ControlFlowGraph.create(segments) }

        // Should be a flat list, not grouped by areaId.
        // Two entries share areaId=17 but they're separate items in the list.
        assertEquals(3, result.size)
        assertEquals(2, result.count { it.areaId == 17 })
    }
}
