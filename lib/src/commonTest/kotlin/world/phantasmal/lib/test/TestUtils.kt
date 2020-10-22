package world.phantasmal.lib.test

import world.phantasmal.core.Success
import world.phantasmal.lib.assembly.InstructionSegment
import world.phantasmal.lib.assembly.assemble
import kotlin.test.assertTrue

fun toInstructions(assembly: String): List<InstructionSegment> {
    val result = assemble(assembly.split('\n'))

    assertTrue(result is Success)
    assertTrue(result.problems.isEmpty())

    return result.value.filterIsInstance<InstructionSegment>()
}
