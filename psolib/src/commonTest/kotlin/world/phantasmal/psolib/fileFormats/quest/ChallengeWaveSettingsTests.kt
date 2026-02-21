package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests to verify challenge mode wave settings parsing.
 */
class ChallengeWaveSettingsTests : LibTestSuite {
    @Test
    fun verify_challenge_event_ids_are_sequential() = testAsync {
        val dat = parseDat(readFile("/chl01.dat"))

        println("\n=== Challenge Mode Event ID Test ===")
        println("Total events: ${dat.events.size}")

        // Print first 10 event IDs to see the pattern
        dat.events.take(10).forEachIndexed { idx, event ->
            println(
                "Event $idx: id=${event.id}, section=${event.sectionId}, " +
                        "wave=${event.wave}, isCM=${event.isChallengeMode}, " +
                        "cmWave=${event.cmWaveSettings}"
            )
        }

        // Verify that event IDs look reasonable (not garbage like 65536, 3932161)
        dat.events.forEach { event ->
            assertTrue(
                event.id in 0..10000,
                "Event ID ${event.id} is out of reasonable range (0-10000)"
            )
        }
    }

    @Test
    fun parse_challenge_mode_wave_settings_chl01() = testAsync {
        val dat = parseDat(readFile("/chl01.dat"))

        println("\n=== Challenge Mode Wave Settings Test (chl01) ===")
        println("Total events parsed: ${dat.events.size}")

        var eventsWithWaveSettings = 0
        var eventsWithActions = 0

        dat.events.forEachIndexed { index, event ->
            println("\nEvent $index (ID=${event.id}):")
            println("  Section: ${event.sectionId}")
            println("  Wave: ${event.wave}")
            println("  Delay: ${event.delay}")
            println("  Is Challenge Mode: ${event.isChallengeMode}")
            println("  CM Wave Settings: ${event.cmWaveSettings}")

            if (event.cmWaveSettings != null) {
                eventsWithWaveSettings++
                val settings = event.cmWaveSettings!!
                println("  CM Min Enemies: ${event.cmMinEnemies}")
                println("  CM Max Enemies: ${event.cmMaxEnemies}")
                println("  CM Max Waves: ${event.cmMaxWaves}")

                // Verify decoding is correct
                assertEquals(settings and 0xFF, event.cmMinEnemies, "Min enemies should match")
                assertEquals((settings shr 8) and 0xFF, event.cmMaxEnemies, "Max enemies should match")
                assertEquals((settings shr 16) and 0xFF, event.cmMaxWaves, "Max waves should match")
            }

            println("  Actions (${event.actions.size}):")
            event.actions.forEach { action ->
                eventsWithActions++
                when (action) {
                    is DatEventAction.SpawnNpcs ->
                        println("    SpawnNpcs: section=${action.sectionId}, flag=${action.appearFlag}")

                    is DatEventAction.Unlock ->
                        println("    Unlock: door=${action.doorId}")

                    is DatEventAction.Lock ->
                        println("    Lock: door=${action.doorId}")

                    is DatEventAction.TriggerEvent ->
                        println("    TriggerEvent: event=${action.eventId}")
                }
            }
        }

        println("\n=== Summary ===")
        println("Events with wave settings: $eventsWithWaveSettings")
        println("Total actions: $eventsWithActions")

        // At least some events should have wave settings in a challenge mode quest
        assertTrue(eventsWithWaveSettings > 0, "Expected at least some events to have CM wave settings")
    }

    @Test
    fun verify_wave_settings_not_all_zero() = testAsync {
        val dat = parseDat(readFile("/chl01.dat"))

        var nonZeroWaveSettings = 0

        dat.events.forEach { event ->
            if (event.cmWaveSettings != null && event.cmWaveSettings != 0) {
                nonZeroWaveSettings++

                // If wave settings is not zero, at least one of the decoded values should be non-zero
                val hasNonZero = event.cmMinEnemies > 0 || event.cmMaxEnemies > 0 || event.cmMaxWaves > 0
                assertTrue(
                    hasNonZero,
                    "Event ${event.id} has cmWaveSettings=${event.cmWaveSettings} but all decoded values are 0"
                )
            }
        }

        println("Events with non-zero wave settings: $nonZeroWaveSettings")
        assertTrue(nonZeroWaveSettings > 0, "Expected at least some events to have non-zero wave settings")
    }

    @Test
    fun verify_unlock_actions_present() = testAsync {
        val dat = parseDat(readFile("/chl01.dat"))

        var unlockCount = 0
        var lockCount = 0
        var spawnCount = 0
        var triggerCount = 0

        println("\n=== Challenge Mode Actions Test ===")
        dat.events.take(10).forEach { event ->
            println("\nEvent ${event.id}:")
            event.actions.forEach { action ->
                when (action) {
                    is DatEventAction.Unlock -> {
                        unlockCount++
                        println("  Unlock door=${action.doorId}")
                    }

                    is DatEventAction.Lock -> {
                        lockCount++
                        println("  Lock door=${action.doorId}")
                    }

                    is DatEventAction.SpawnNpcs -> {
                        spawnCount++
                        println("  SpawnNpcs section=${action.sectionId}, flag=${action.appearFlag}")
                    }

                    is DatEventAction.TriggerEvent -> {
                        triggerCount++
                        println("  TriggerEvent event=${action.eventId}")
                    }
                }
            }
        }

        println("\n=== Action Summary ===")
        println("Unlock: $unlockCount")
        println("Lock: $lockCount")
        println("SpawnNpcs: $spawnCount")
        println("TriggerEvent: $triggerCount")

        // Challenge mode quests should have SOME actions
        val totalActions = unlockCount + lockCount + spawnCount + triggerCount
        assertTrue(totalActions > 0, "Expected to find some actions, found $totalActions")
    }

    @Test
    fun debug_cursor_positions_chl01() = testAsync {
        val cursor = readFile("/chl01.dat")

        println("\n=== Debugging Cursor Positions ===")

        // Read the event section header
        cursor.seekStart(0)
        val entityType = cursor.int()
        println("Entity type: $entityType (expected: 3)")

        val totalSize = cursor.int()
        println("Total size: $totalSize")

        val areaId = cursor.int()
        println("Area ID: $areaId")

        val entitiesSize = cursor.int()
        println("Entities size: $entitiesSize")

        // Event header
        val startPos = cursor.position
        println("\nEvent section starts at: $startPos")

        val actionsOffset = cursor.int()
        println("Actions offset (relative): $actionsOffset")
        println("Actions offset (absolute): ${startPos + actionsOffset}")

        cursor.seek(4) // 0x10
        val eventCount = cursor.int()
        println("Event count: $eventCount")

        cursor.seek(3) // 0
        val eventType = cursor.byte()
        println("Event type: 0x${eventType.toString(16)}")

        val isCM = eventType.toInt() == 0x32
        println("Is challenge mode: $isCM")
        println("Expected event size: ${if (isCM) 28 else 20}")

        // Now we're at position startPos + 16, which is where events start
        println("\nEvent data starts at: ${cursor.position}")

        // Read first event manually
        println("\n=== Reading Event 0 ===")
        val event0Start = cursor.position
        println("Event 0 start position: $event0Start")

        val event0Id = cursor.int()
        println("Event 0 ID: $event0Id (cursor now at ${cursor.position})")

        val event0Const = cursor.int()
        println("Event 0 constant: 0x${event0Const.toString(16)} (cursor now at ${cursor.position})")

        val event0Section = cursor.short()
        println("Event 0 section: $event0Section (cursor now at ${cursor.position})")

        val event0Wave = cursor.short()
        println("Event 0 wave: $event0Wave (cursor now at ${cursor.position})")

        val event0Delay = cursor.short()
        println("Event 0 delay: $event0Delay (cursor now at ${cursor.position})")

        val event0Unknown = cursor.short()
        println("Event 0 unknown: 0x${event0Unknown.toString(16)} (cursor now at ${cursor.position})")

        if (isCM) {
            val event0WaveSettings = cursor.int()
            println("Event 0 wave settings: 0x${event0WaveSettings.toString(16)} (cursor now at ${cursor.position})")

            val event0WaveChoice = cursor.int()
            println("Event 0 wave choice: $event0WaveChoice (cursor now at ${cursor.position})")
        }

        val event0ActionsOffset = cursor.int()
        println("Event 0 actions offset: $event0ActionsOffset (cursor now at ${cursor.position})")

        val event0End = cursor.position
        val event0BytesRead = event0End - event0Start
        println("Event 0 bytes read: $event0BytesRead (expected: 28)")

        // Read second event manually
        println("\n=== Reading Event 1 ===")
        val event1Start = cursor.position
        println("Event 1 start position: $event1Start (expected: ${event0Start + 28})")

        val event1Id = cursor.int()
        println("Event 1 ID: $event1Id (cursor now at ${cursor.position})")

        if (event1Id == 65536) {
            println("ERROR: Event 1 ID is 65536 (0x10000) - this is the constant, not the ID!")
            println("This means we're 4 bytes ahead of where we should be")
        }
    }
}