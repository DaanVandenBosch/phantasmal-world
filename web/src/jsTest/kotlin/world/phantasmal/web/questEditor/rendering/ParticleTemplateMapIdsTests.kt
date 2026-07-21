package world.phantasmal.web.questEditor.rendering

import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawn
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawnOpcode
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawnOrigin
import world.phantasmal.psolib.asm.dataFlowAnalysis.ParticleSpawnSource
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class ParticleTemplateMapIdsTests : WebTestSuite {
    @Test
    fun resolved_invocation_is_visible_only_on_an_execution_floor() = test {
        val floor1 = spawn(100, setOf(1))
        val floor2 = spawn(101, setOf(2))

        assertEquals(listOf(floor1), particleSpawnsForFloorView(listOf(floor1, floor2), setOf(1)))
        assertEquals(listOf(floor2), particleSpawnsForFloorView(listOf(floor1, floor2), setOf(2)))
    }

    @Test
    fun shared_handler_invocation_is_visible_on_each_reachable_floor() = test {
        val shared = spawn(100, setOf(1, 2))

        assertEquals(listOf(shared), particleSpawnsForFloorView(listOf(shared), setOf(1)))
        assertEquals(listOf(shared), particleSpawnsForFloorView(listOf(shared), setOf(2)))
        assertEquals(emptyList(), particleSpawnsForFloorView(listOf(shared), setOf(3)))
    }

    @Test
    fun unresolved_execution_floor_is_not_shown_on_an_arbitrary_floor() = test {
        val unresolved = spawn(100, emptySet())

        assertEquals(emptyList(), particleSpawnsForFloorView(listOf(unresolved), setOf(7)))
    }

    @Test
    fun global_template_uses_one_available_map_without_floor_attribution() = test {
        assertEquals(
            setOf(9),
            particleTemplateMapIds(
                spawn(100, setOf(1, 2)),
                mapOf(1 to 4, 2 to 5),
                setOf(1),
                9,
            ),
        )
    }

    @Test
    fun map_local_template_uses_only_the_visible_execution_floor_map() = test {
        assertEquals(
            setOf(4),
            particleTemplateMapIds(
                spawn(512, setOf(1, 2)),
                mapOf(1 to 4, 2 to 5),
                setOf(1),
                9,
            ),
        )
    }

    @Test
    fun grouped_floor_view_uses_each_visible_execution_floor_map() = test {
        assertEquals(
            setOf(4, 5),
            particleTemplateMapIds(
                spawn(512, setOf(1, 2, 3)),
                mapOf(1 to 4, 2 to 5, 3 to 6),
                setOf(1, 2),
                9,
            ),
        )
    }

    @Test
    fun map_local_template_without_a_visible_mapping_uses_current_map() = test {
        assertEquals(
            setOf(9),
            particleTemplateMapIds(spawn(575, emptySet()), emptyMap(), setOf(7), 9),
        )
    }

    private fun spawn(particleId: Int, floorHints: Set<Int>) = ParticleSpawn(
        origin = ParticleSpawnOrigin.WorldPosition(0, 0, 0),
        particleId = particleId,
        lifetimeFrames = 30,
        source = ParticleSpawnSource.Opcode(ParticleSpawnOpcode.ParticleV3),
        hasExtendedDrawRange = false,
        executionFloorIds = floorHints,
    )
}
