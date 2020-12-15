package world.phantasmal.lib.test

import world.phantasmal.core.Success
import world.phantasmal.lib.asm.InstructionSegment
import world.phantasmal.lib.asm.assemble
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import kotlin.test.assertEquals
import kotlin.test.assertTrue

expect suspend fun readFile(path: String): Cursor

fun toInstructions(assembly: String): List<InstructionSegment> {
    val result = assemble(assembly.split('\n'))

    assertTrue(result is Success)
    assertTrue(result.problems.isEmpty())

    return result.value.instructionSegments()
}

fun <T> assertDeepEquals(expected: List<T>, actual: List<T>, assertDeepEquals: (T, T) -> Unit) {
    assertEquals(expected.size, actual.size)

    for (i in actual.indices) {
        assertDeepEquals(expected[i], actual[i])
    }
}

fun assertDeepEquals(expected: Buffer, actual: Buffer): Boolean {
    if (expected.size != actual.size) return false

    for (i in 0 until expected.size) {
        if (expected.getByte(i) != actual.getByte(i)) return false
    }

    return true
}
