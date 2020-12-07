package world.phantasmal.lib.asm

import world.phantasmal.core.Success
import world.phantasmal.lib.test.LibTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssemblyTests : LibTestSuite() {
    @Test
    fun assemble_basic_script() {
        val result = assemble("""
            0:
                set_episode 0
                set_floor_handler 0, 150
                set_floor_handler 1, 151
                set_qt_success 250
                bb_map_designate 0, 0, 0, 0
                bb_map_designate 1, 1, 0, 0
                ret
            1:
                ret
            250:
                gset 101
                window_msg "You've been awarded 500 Meseta."
                bgm 1
                winend
                pl_add_meseta 0, 500
                ret
        """.trimIndent().split('\n'))

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())
        assertEquals(3, result.value.segments.size)
    }
}
