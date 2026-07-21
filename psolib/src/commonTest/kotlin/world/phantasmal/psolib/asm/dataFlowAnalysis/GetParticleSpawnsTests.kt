package world.phantasmal.psolib.asm.dataFlowAnalysis

import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.toInstructions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val ParticleSpawn.worldPosition: ParticleSpawnOrigin.WorldPosition
    get() = origin as ParticleSpawnOrigin.WorldPosition

class GetParticleSpawnsTests : LibTestSuite {
    @Test
    fun particle_inside_floor_handler_is_attributed_to_that_floor() {
        val segments = toInstructions("""
            0:
                set_floor_handler 5, 100
                ret
            100:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 42
                leti r4, 60
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        val spawn = spawns[0]
        assertEquals(1000, spawn.worldPosition.x)
        assertEquals(0, spawn.worldPosition.y)
        assertEquals(2000, spawn.worldPosition.z)
        assertEquals(42, spawn.particleId)
        assertEquals(60, spawn.lifetimeFrames)
        assertEquals(ParticleSpawnSource.Opcode(ParticleSpawnOpcode.ParticleV3), spawn.source)
        assertTrue(!spawn.hasExtendedDrawRange)
        assertEquals(setOf(5), spawn.executionFloorIds)
    }

    @Test
    fun resolves_all_psobb_particle_opcode_variants() {
        val segments = toInstructions("""
            0:
                leti r0, 100
                leti r1, 200
                leti r2, 300
                particle2 r0, 513, 29.6

                leti r10, 7
                leti r11, 60
                leti r12, 4099
                leti r13, 25
                particle_id_v3 r10

                leti r20, -100
                leti r21, 0
                leti r22, 500
                leti r23, 65535
                leti r24, 90
                particle_effect_nc r20

                leti r30, 8
                leti r31, 120
                leti r32, 81925
                leti r33, -10
                player_effect_nc r30
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(4, spawns.size)

        assertEquals(ParticleSpawnSource.Opcode(ParticleSpawnOpcode.Particle2), spawns[0].source)
        assertEquals(ParticleSpawnOrigin.WorldPosition(100, 200, 300), spawns[0].origin)
        assertEquals(513, spawns[0].particleId)
        assertEquals(29, spawns[0].lifetimeFrames)
        assertTrue(spawns[0].hasExtendedDrawRange)

        assertEquals(ParticleSpawnSource.Opcode(ParticleSpawnOpcode.ParticleIdV3), spawns[1].source)
        assertEquals(ParticleSpawnOrigin.EntityPosition(0x1003, 25), spawns[1].origin)
        assertTrue(!spawns[1].hasExtendedDrawRange)

        assertEquals(ParticleSpawnSource.Opcode(ParticleSpawnOpcode.ParticleEffectNoCull), spawns[2].source)
        assertEquals(ParticleSpawnOrigin.WorldPosition(-100, 0, 500), spawns[2].origin)
        assertEquals(-1, spawns[2].particleId)
        assertTrue(spawns[2].hasExtendedDrawRange)

        assertEquals(ParticleSpawnSource.Opcode(ParticleSpawnOpcode.PlayerEffectNoCull), spawns[3].source)
        assertEquals(ParticleSpawnOrigin.EntityPosition(0x4005, -10), spawns[3].origin)
        assertTrue(spawns[3].hasExtendedDrawRange)
    }

    @Test
    fun particle_inside_at_coords_call_callback_is_attributed_to_registration_floor() {
        // Floor 7's handler registers at_coords_call with label 200 as the body. Because
        // the trigger geometry (XYZ + radius) is created on the registration floor, the
        // body fires only on that floor — so the spawn must inherit floor 7.
        val segments = toInstructions("""
            0:
                set_floor_handler 7, 100
                ret
            100:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 50
                leti r4, 200
                at_coords_call r0
                ret
            200:
                leti r10, 5000
                leti r11, 0
                leti r12, 6000
                leti r13, 1
                leti r14, 30
                particle_v3 r10
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(7), spawns[0].executionFloorIds)
    }

    @Test
    fun particle_inside_at_coords_talk_callback_is_attributed_to_registration_floor() {
        // Same expectation as at_coords_call: at_coords_talk's trigger is per-floor.
        val segments = toInstructions("""
            0:
                set_floor_handler 4, 100
                ret
            100:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 50
                leti r4, 250
                at_coords_talk r0
                ret
            250:
                leti r10, 5000
                leti r11, 0
                leti r12, 6000
                leti r13, 1
                leti r14, 30
                particle_v3 r10
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(4), spawns[0].executionFloorIds)
    }

    @Test
    fun particle_inside_set_qt_success_callback_is_attributed_to_pioneer_2() {
        // set_qt_success bodies fire at the Hunter's Guild on Pioneer 2, not on the floor
        // where the registration happened.
        val segments = toInstructions("""
            0:
                set_floor_handler 11, 100
                ret
            100:
                set_qt_success 300
                ret
            300:
                leti r10, 100
                leti r11, 0
                leti r12, 200
                leti r13, 1
                leti r14, 30
                particle_v3 r10
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(0), spawns[0].executionFloorIds)
    }

    @Test
    fun cleared_quest_success_handler_is_not_started() {
        val segments = toInstructions("""
            0:
                set_qt_success 300
                clr_qt_success
                ret
            300:
                leti r10, 100
                leti r11, 0
                leti r12, 200
                leti r13, 1
                leti r14, 30
                particle_v3 r10
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertTrue(spawns.isEmpty())
    }

    @Test
    fun quest_exit_handler_uses_the_runtime_exit_floor() {
        val segments = toInstructions("""
            0:
                set_qt_exit 300
                ret
            300:
                leti r10, 100
                leti r11, 0
                leti r12, 200
                leti r13, 1
                leti r14, 30
                particle_v3 r10
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals((0 until 0x12).toSet(), spawns.single().executionFloorIds)
    }

    @Test
    fun set_floor_handler_in_nested_segment_is_recognized() {
        // Quests don't have to register handlers from label 0. Label 0 calls a helper
        // that does the registration; the scan must cover all segments to discover it.
        val segments = toInstructions("""
            0:
                call 500
                ret
            500:
                set_floor_handler 9, 100
                ret
            100:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(9), spawns[0].executionFloorIds)
    }

    @Test
    fun later_floor_handler_replaces_the_previous_handler() {
        val segments = toInstructions("""
            0:
                set_floor_handler 4, 100
                set_floor_handler 4, 200
                ret
            100:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
            200:
                leti r10, 5000
                leti r11, 0
                leti r12, 6000
                leti r13, 2
                leti r14, 60
                particle_v3 r10
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(5000, spawns.single().worldPosition.x)
        assertEquals(setOf(4), spawns.single().executionFloorIds)
    }

    @Test
    fun cleared_floor_handler_is_not_started() {
        val segments = toInstructions("""
            0:
                set_floor_handler 4, 100
                clr_floor_handler 4
                ret
            100:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertTrue(spawns.isEmpty())
    }

    @Test
    fun set_floor_handler_in_unreachable_code_is_not_registered() {
        val segments = toInstructions("""
            0:
                ret
            500:
                set_floor_handler 9, 100
                ret
            100:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertTrue(spawns.isEmpty())
    }

    @Test
    fun particle_after_ret_is_not_a_client_emitter() {
        val segments = toInstructions("""
            0:
                ret
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertTrue(spawns.isEmpty())
    }

    @Test
    fun particle_inside_thread_callback_is_attributed_to_registration_floor() {
        // thread_stg reparents the thread to the client's floor-local object list. It can yield
        // repeatedly, but a floor transition destroys it before it can execute on another floor.
        val segments = toInstructions("""
            0:
                set_floor_handler 4, 100
                ret
            100:
                thread_stg 200
                ret
            200:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(4), spawns[0].executionFloorIds)
    }

    @Test
    fun floor_scoped_thread_remains_fixed_after_sync() {
        val segments = toInstructions("""
            0:
                set_floor_handler 4, 100
                ret
            100:
                thread_stg 200
                ret
            200:
                sync
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(setOf(4), spawns.single().executionFloorIds)
    }

    @Test
    fun ordinary_thread_is_fixed_before_its_first_yield() {
        val segments = toInstructions("""
            0:
                set_floor_handler 6, 100
                ret
            100:
                thread 200
                ret
            200:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                sync
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(setOf(6), spawns.single().executionFloorIds)
    }

    @Test
    fun persistent_floor_handler_resumes_on_the_runtime_current_floor() {
        val segments = toInstructions("""
            0:
                set_floor_handler 6, 100
                ret
            100:
                sync
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        // StartQuestThreadForCurrentFloor creates an ordinary QuestThread2, so it is not
        // destroyed with the old floor's object list and may resume after a transition.
        assertEquals((0 until 0x12).toSet(), spawns.single().executionFloorIds)
    }

    @Test
    fun resumed_thread_floor_check_restores_a_single_floor_attribution() {
        val segments = toInstructions("""
            0:
                set_floor_handler 6, 100
                ret
            100:
                sync
                get_floor_number 0, r10
                jmpi_= r10, 6, 200
                ret
            200:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(setOf(6), spawns.single().executionFloorIds)
    }

    @Test
    fun ordinary_looping_thread_uses_the_runtime_current_floor_after_sync() {
        val segments = toInstructions("""
            0:
                set_floor_handler 6, 100
                ret
            100:
                thread 200
                ret
            200:
                sync
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                jmp 200
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        // PSOBB leaves an ordinary thread parented to the Quest object. Unlike thread_stg,
        // it is not destroyed with g_QuestThreadListHead during a floor transition.
        assertEquals((0 until 0x12).toSet(), spawns.single().executionFloorIds)
    }

    @Test
    fun ordinary_thread_registrations_are_applied_without_fixing_the_thread_to_a_floor() {
        val segments = toInstructions("""
            0:
                thread 200
                ret
            200:
                sync
                set_floor_handler 7, 300
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
            300:
                leti r10, 5000
                leti r11, 0
                leti r12, 6000
                leti r13, 2
                leti r14, 60
                particle_v3 r10
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(2, spawns.size)
        assertEquals(
            (0 until 0x12).toSet(),
            spawns.single { it.worldPosition.x == 1000 }.executionFloorIds,
        )
        assertEquals(
            setOf(7),
            spawns.single { it.worldPosition.x == 5000 }.executionFloorIds,
        )
    }

    @Test
    fun shared_handler_label_propagates_all_registered_floors() {
        // Many quests share one handler across multiple floors via repeated set_floor_handler
        // calls (e.g. an "ambience" handler used for floors 1..3). All registered floors must
        // be propagated to particles reachable from that handler — not just the last one
        // written.
        val segments = toInstructions("""
            0:
                set_floor_handler 1, 100
                set_floor_handler 2, 100
                set_floor_handler 3, 100
                ret
            100:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(1, 2, 3), spawns[0].executionFloorIds)
    }

    @Test
    fun shared_helper_does_not_pollute_floors_through_callee_return_edges() {
        // Floor 1 calls helper L500 then runs floor-1-specific code (containing a particle).
        // Floor 2 calls L500 then runs floor-2-specific code (containing a different particle).
        // The CFG's linkReturningBlocks would normally let BFS from floor 1's handler walk
        // through L500's ret block back into floor 2's after-call code; the analysis must
        // exclude those edges and use the call site's own segment-sequential successor.
        val segments = toInstructions("""
            0:
                set_floor_handler 1, 100
                set_floor_handler 2, 200
                ret
            100:
                call 500
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
            200:
                call 500
                leti r10, 5000
                leti r11, 0
                leti r12, 6000
                leti r13, 2
                leti r14, 60
                particle_v3 r10
                ret
            500:
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(2, spawns.size)
        val floor1Spawn = spawns.firstOrNull { it.worldPosition.x == 1000 }
        assertEquals(setOf(1), floor1Spawn?.executionFloorIds, "Floor 1 spawn should not be tagged with floor 2")
        val floor2Spawn = spawns.firstOrNull { it.worldPosition.x == 5000 }
        assertEquals(setOf(2), floor2Spawn?.executionFloorIds, "Floor 2 spawn should not be tagged with floor 1")
    }

    @Test
    fun shared_handler_with_switch_jmp_on_floor_register_attributes_per_floor() {
        // A handler is registered for floors 0, 1 and 2. Inside, get_floor_number is read
        // into a register and switch_jmp dispatches to per-floor branches. Path-sensitive
        // analysis on the floor register should attribute each particle_v3 to a single floor.
        //
        // Note on switch_jmp index alignment: switch_jmp uses the register's value as an
        // index into its label list (0-based). Our per-floor BFS picks args[currentFloor + 1]
        // as the target label, so we use floor IDs 0, 1, 2 here for clean indexing into a
        // small label list.
        val segments = toInstructions("""
            0:
                set_floor_handler 0, 100
                set_floor_handler 1, 100
                set_floor_handler 2, 100
                ret
            100:
                get_floor_number 0, r10
                switch_jmp r10, 200, 201, 202
                ret
            200:
                leti r20, 1000
                leti r21, 0
                leti r22, 2000
                leti r23, 1
                leti r24, 30
                particle_v3 r20
                ret
            201:
                leti r30, 5000
                leti r31, 0
                leti r32, 6000
                leti r33, 2
                leti r34, 60
                particle_v3 r30
                ret
            202:
                leti r40, 9000
                leti r41, 0
                leti r42, 10000
                leti r43, 3
                leti r44, 90
                particle_v3 r40
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(3, spawns.size)

        val floor0Spawn = spawns.firstOrNull { it.worldPosition.x == 1000 }
        assertEquals(setOf(0), floor0Spawn?.executionFloorIds, "Floor 0 spawn should be attributed only to floor 0")

        val floor1Spawn = spawns.firstOrNull { it.worldPosition.x == 5000 }
        assertEquals(setOf(1), floor1Spawn?.executionFloorIds, "Floor 1 spawn should be attributed only to floor 1")

        val floor2Spawn = spawns.firstOrNull { it.worldPosition.x == 9000 }
        assertEquals(setOf(2), floor2Spawn?.executionFloorIds, "Floor 2 spawn should be attributed only to floor 2")
    }

    @Test
    fun shared_handler_with_jmpi_e_on_floor_register_attributes_per_floor() {
        // Same idea but using jmpi_= instead of switch_jmp.
        val segments = toInstructions("""
            0:
                set_floor_handler 1, 100
                set_floor_handler 2, 100
                ret
            100:
                get_floor_number 0, r10
                jmpi_= r10, 1, 200
                jmp 201
            200:
                leti r20, 1000
                leti r21, 0
                leti r22, 2000
                leti r23, 1
                leti r24, 30
                particle_v3 r20
                ret
            201:
                leti r30, 5000
                leti r31, 0
                leti r32, 6000
                leti r33, 2
                leti r34, 60
                particle_v3 r30
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(2, spawns.size)

        val floor1Spawn = spawns.firstOrNull { it.worldPosition.x == 1000 }
        assertEquals(setOf(1), floor1Spawn?.executionFloorIds)

        val floor2Spawn = spawns.firstOrNull { it.worldPosition.x == 5000 }
        assertEquals(setOf(2), floor2Spawn?.executionFloorIds)
    }

    @Test
    fun shared_handler_with_jmpi_ne_on_floor_register_attributes_per_floor() {
        val segments = toInstructions("""
            0:
                set_floor_handler 1, 100
                set_floor_handler 2, 100
                ret
            100:
                get_floor_number 0, r10
                jmpi_!= r10, 1, 201
                jmp 200
            200:
                leti r20, 1000
                leti r21, 0
                leti r22, 2000
                leti r23, 1
                leti r24, 30
                particle_v3 r20
                ret
            201:
                leti r30, 5000
                leti r31, 0
                leti r32, 6000
                leti r33, 2
                leti r34, 60
                particle_v3 r30
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(2, spawns.size)
        assertEquals(
            setOf(1),
            spawns.firstOrNull { it.worldPosition.x == 1000 }?.executionFloorIds,
        )
        assertEquals(
            setOf(2),
            spawns.firstOrNull { it.worldPosition.x == 5000 }?.executionFloorIds,
        )
    }

    @Test
    fun literal_equal_to_floor_does_not_prune_unrelated_branch() {
        val segments = toInstructions("""
            0:
                set_floor_handler 5, 100
                ret
            100:
                leti r10, 5
                jmpi_= r10, 1, 200
                ret
            200:
                leti r20, 1000
                leti r21, 0
                leti r22, 2000
                leti r23, 1
                leti r24, 30
                particle_v3 r20
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(5), spawns[0].executionFloorIds)
    }

    @Test
    fun shared_handler_with_switch_call_on_floor_register_attributes_per_floor() {
        val segments = toInstructions("""
            0:
                set_floor_handler 0, 100
                set_floor_handler 1, 100
                ret
            100:
                get_floor_number 0, r10
                switch_call r10, 200, 201
                ret
            200:
                leti r20, 1000
                leti r21, 0
                leti r22, 2000
                leti r23, 1
                leti r24, 30
                particle_v3 r20
                ret
            201:
                leti r30, 5000
                leti r31, 0
                leti r32, 6000
                leti r33, 2
                leti r34, 60
                particle_v3 r30
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(2, spawns.size)
        assertEquals(
            setOf(0),
            spawns.firstOrNull { it.worldPosition.x == 1000 }?.executionFloorIds,
        )
        assertEquals(
            setOf(1),
            spawns.firstOrNull { it.worldPosition.x == 5000 }?.executionFloorIds,
        )
    }

    @Test
    fun helper_setting_floor_register_propagates_state_back_to_caller() {
        // Interprocedural propagation: a shared handler calls a helper that sets the floor
        // register via get_floor_number, then dispatches on it. The floor-register taint
        // must survive the call/return boundary so the caller's switch_jmp can be pruned.
        val segments = toInstructions("""
            0:
                set_floor_handler 0, 100
                set_floor_handler 1, 100
                set_floor_handler 2, 100
                ret
            100:
                call 200
                switch_jmp r10, 300, 301, 302
                ret
            200:
                get_floor_number 0, r10
                ret
            300:
                leti r20, 1000
                leti r21, 0
                leti r22, 2000
                leti r23, 1
                leti r24, 30
                particle_v3 r20
                ret
            301:
                leti r30, 5000
                leti r31, 0
                leti r32, 6000
                leti r33, 2
                leti r34, 60
                particle_v3 r30
                ret
            302:
                leti r40, 9000
                leti r41, 0
                leti r42, 10000
                leti r43, 3
                leti r44, 90
                particle_v3 r40
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(3, spawns.size)
        assertEquals(setOf(0), spawns.firstOrNull { it.worldPosition.x == 1000 }?.executionFloorIds, "floor 0 spawn")
        assertEquals(setOf(1), spawns.firstOrNull { it.worldPosition.x == 5000 }?.executionFloorIds, "floor 1 spawn")
        assertEquals(setOf(2), spawns.firstOrNull { it.worldPosition.x == 9000 }?.executionFloorIds, "floor 2 spawn")
    }

    @Test
    fun particle_reachable_from_two_floor_handlers_gets_union_of_floors() {
        // A helper called from two different floor handlers should be tagged with both
        // floors — at runtime the helper can fire on either floor.
        val segments = toInstructions("""
            0:
                set_floor_handler 2, 100
                set_floor_handler 5, 200
                ret
            100:
                call 999
                ret
            200:
                call 999
                ret
            999:
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(2, 5), spawns[0].executionFloorIds)
    }

    @Test
    fun particle_in_label_0_init_code_is_attributed_to_floor_0() {
        // The client runs label 0 once during quest setup on Pioneer 2 before starting the
        // current floor handler.
        val segments = toInstructions("""
            0:
                set_floor_handler 3, 100
                leti r0, 1000
                leti r1, 0
                leti r2, 2000
                leti r3, 1
                leti r4, 30
                particle_v3 r0
                ret
            100:
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(1, spawns.size)
        assertEquals(setOf(0), spawns[0].executionFloorIds)
    }

    @Test
    fun runtime_player_coordinates_are_not_misread_as_fixed_constants() {
        val segments = toInstructions("""
            0:
                leti r0, 100
                leti r1, 200
                leti r2, 300
                get_coord_of_player r0, r250
                leti r3, 18
                leti r4, 60
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertTrue(spawns.isEmpty())
    }

    @Test
    fun dat_entity_script_entry_point_supplies_its_floor() {
        val segments = toInstructions("""
            0:
                ret
            200:
                leti r0, 100
                leti r1, 200
                leti r2, 300
                leti r3, 42
                leti r4, 60
                particle_v3 r0
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(
            segments,
            entityEntryPointFloorIds = mapOf(200 to setOf(7)),
        ) { ControlFlowGraph.create(segments) }

        assertEquals(setOf(7), spawns.single().executionFloorIds)
    }

    @Test
    fun party_coordinate_callback_uses_its_sixth_register_as_the_label() {
        val segments = toInstructions("""
            0:
                set_floor_handler 4, 100
                ret
            100:
                leti r0, 10
                leti r1, 20
                leti r2, 30
                leti r3, 40
                leti r4, 50
                leti r5, 200
                col_npcinr r0
                ret
            200:
                leti r10, 100
                leti r11, 200
                leti r12, 300
                leti r13, 42
                leti r14, 60
                particle_v3 r10
                ret
        """.trimIndent())

        val spawns = getParticleSpawns(segments) { ControlFlowGraph.create(segments) }

        assertEquals(setOf(4), spawns.single().executionFloorIds)
    }
}
