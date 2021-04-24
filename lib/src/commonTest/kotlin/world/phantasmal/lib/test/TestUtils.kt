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

fun assertDeepEquals(expected: Buffer, actual: Buffer, message: String? = null) {
    assertEquals(
        expected.size,
        actual.size,
        "Unexpected buffer size" + (if (message == null) "" else ". $message"),
    )

    for (i in 0 until expected.size) {
        assertEquals(expected.getByte(i), actual.getByte(i), message)
    }
}

fun assertDeepEquals(expected: Cursor, actual: Cursor) {
    assertEquals(expected.size, actual.size, "Unexpected cursor size")

    while (expected.hasBytesLeft()) {
        assertEquals(expected.byte(), actual.byte())
    }
}
