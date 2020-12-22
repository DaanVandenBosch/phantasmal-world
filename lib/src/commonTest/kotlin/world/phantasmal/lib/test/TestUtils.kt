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

    for (i in expected.indices) {
        assertDeepEquals(expected[i], actual[i])
    }
}

fun <K, V> assertDeepEquals(
    expected: Map<K, V>,
    actual: Map<K, V>,
    assertDeepEquals: (V, V) -> Unit,
) {
    assertEquals(expected.size, actual.size)

    for ((key, value) in expected) {
        assertTrue(key in actual)
        assertDeepEquals(value, actual[key]!!)
    }
}

fun assertDeepEquals(expected: Buffer, actual: Buffer) {
    assertEquals(expected.size, actual.size)

    for (i in 0 until expected.size) {
        assertEquals(expected.getByte(i), actual.getByte(i))
    }
}

fun assertDeepEquals(expected: Cursor, actual: Cursor) {
    assertEquals(expected.size, actual.size)

    while (expected.hasBytesLeft()) {
        assertEquals(expected.byte(), actual.byte())
    }
}
