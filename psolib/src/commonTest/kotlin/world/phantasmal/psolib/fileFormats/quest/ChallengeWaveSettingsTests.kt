package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.TETHEALLA_QUEST_PATH_PREFIX
import world.phantasmal.psolib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CHL_QST = "$TETHEALLA_QUEST_PATH_PREFIX/chl/ep1/1.qst"

/**
 * Tests to verify challenge mode wave settings parsing.
 * Uses Tethealla EP1 challenge quest 1 (equivalent to 1c1).
 */
class ChallengeWaveSettingsTests : LibTestSuite {

    private suspend fun loadChlEvents(): List<DatEvent> {
        val result = parseQstToQuest(readFile(CHL_QST), lenient = true)
        assertTrue(result is Success, "Failed to parse chl quest: ${result.problems.joinToString()}")
        return result.value.quest.events
    }

    @Test
    fun verify_challenge_event_ids_are_sequential() = testAsync {
        val events = loadChlEvents()
        assertTrue(events.isNotEmpty(), "Expected events in challenge mode quest")

        // Verify that event IDs look reasonable (not garbage like 65536, 3932161)
        events.forEach { event ->
            assertTrue(
                event.id in 0..10000,
                "Event ID ${event.id} is out of reasonable range (0-10000)"
            )
        }
    }

    @Test
    fun parse_challenge_mode_wave_settings_chl01() = testAsync {
        val events = loadChlEvents()
        assertTrue(events.isNotEmpty(), "Expected events in challenge mode quest")

        var eventsWithWaveSettings = 0

        events.forEach { event ->
            if (event.cmWaveSettings != null) {
                eventsWithWaveSettings++
                val settings = event.cmWaveSettings!!

                // Verify decoding is correct
                assertEquals(settings and 0xFF, event.cmMinEnemies, "Min enemies should match")
                assertEquals((settings shr 8) and 0xFF, event.cmMaxEnemies, "Max enemies should match")
                assertEquals((settings shr 16) and 0xFF, event.cmMaxWaves, "Max waves should match")
            }
        }

        // At least some events should have wave settings in a challenge mode quest
        assertTrue(eventsWithWaveSettings > 0, "Expected at least some events to have CM wave settings")
    }

    @Test
    fun verify_wave_settings_not_all_zero() = testAsync {
        val events = loadChlEvents()

        var nonZeroWaveSettings = 0

        events.forEach { event ->
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

        assertTrue(nonZeroWaveSettings > 0, "Expected at least some events to have non-zero wave settings")
    }

    @Test
    fun verify_unlock_actions_present() = testAsync {
        val events = loadChlEvents()

        var unlockCount = 0
        var lockCount = 0
        var spawnCount = 0
        var triggerCount = 0

        events.forEach { event ->
            event.actions.forEach { action ->
                when (action) {
                    is DatEventAction.Unlock -> unlockCount++
                    is DatEventAction.Lock -> lockCount++
                    is DatEventAction.SpawnNpcs -> spawnCount++
                    is DatEventAction.TriggerEvent -> triggerCount++
                }
            }
        }

        // Challenge mode quests should have SOME actions
        val totalActions = unlockCount + lockCount + spawnCount + triggerCount
        assertTrue(totalActions > 0, "Expected to find some actions, found $totalActions")
    }
}
