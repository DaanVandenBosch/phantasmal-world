package world.phantasmal.lib.test

import world.phantasmal.core.Success
import world.phantasmal.lib.assembly.InstructionSegment
import world.phantasmal.lib.assembly.assemble
import world.phantasmal.lib.cursor.Cursor
import kotlin.test.assertTrue

/**
 * Ensure you return the value of this function in your test function. On Kotlin/JS this function
 * actually returns a Promise. If this promise is not returned from the test function, the testing
 * framework won't wait for its completion. This is a workaround for issue
 * [https://youtrack.jetbrains.com/issue/KT-22228].
 */
expect fun asyncTest(block: suspend () -> Unit)

expect suspend fun readFile(path: String): Cursor

fun toInstructions(assembly: String): List<InstructionSegment> {
    val result = assemble(assembly.split('\n'))

    assertTrue(result is Success)
    assertTrue(result.problems.isEmpty())

    return result.value.filterIsInstance<InstructionSegment>()
}
