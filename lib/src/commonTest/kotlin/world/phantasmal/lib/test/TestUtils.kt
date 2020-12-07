package world.phantasmal.lib.test

import world.phantasmal.core.Success
import world.phantasmal.lib.asm.InstructionSegment
import world.phantasmal.lib.asm.assemble
import world.phantasmal.lib.cursor.Cursor
import kotlin.test.assertTrue

expect suspend fun readFile(path: String): Cursor

fun toInstructions(assembly: String): List<InstructionSegment> {
    val result = assemble(assembly.split('\n'))

    assertTrue(result is Success)
    assertTrue(result.problems.isEmpty())

    return result.value.instructionSegments()
}
