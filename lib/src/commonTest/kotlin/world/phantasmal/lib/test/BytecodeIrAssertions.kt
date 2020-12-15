package world.phantasmal.lib.test

import world.phantasmal.lib.asm.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

fun assertDeepEquals(expected: BytecodeIr, actual: BytecodeIr, ignoreSrcLocs: Boolean = false) {
    assertDeepEquals(expected.segments,
        actual.segments
    ) { a, b -> assertDeepEquals(a, b, ignoreSrcLocs) }
}

fun assertDeepEquals(expected: Segment, actual: Segment, ignoreSrcLocs: Boolean = false) {
    assertEquals(expected::class, actual::class)
    assertDeepEquals(expected.labels, actual.labels, ::assertEquals)

    if (!ignoreSrcLocs) {
        assertDeepEquals(expected.srcLoc, actual.srcLoc)
    }

    when (expected) {
        is InstructionSegment -> {
            actual as InstructionSegment
            assertDeepEquals(expected.instructions, actual.instructions) { a, b ->
                assertDeepEquals(a, b, ignoreSrcLocs)
            }
        }
        is DataSegment -> {
            actual as DataSegment
            assertDeepEquals(expected.data, actual.data)
        }
        is StringSegment -> {
            actual as StringSegment
            assertEquals(expected.value, actual.value)
        }
    }
}

fun assertDeepEquals(expected: Instruction, actual: Instruction, ignoreSrcLocs: Boolean = false) {
    assertEquals(expected.opcode, actual.opcode)
    assertDeepEquals(expected.args, actual.args, ::assertEquals)

    if (!ignoreSrcLocs) {
        assertDeepEquals(expected.srcLoc, actual.srcLoc)
    }
}

fun assertDeepEquals(expected: SrcLoc?, actual: SrcLoc?) {
    if (expected == null) {
        assertNull(actual)
        return
    }

    assertNotNull(actual)
    assertEquals(expected.lineNo, actual.lineNo)
    assertEquals(expected.col, actual.col)
    assertEquals(expected.len, actual.len)
}

fun assertDeepEquals(expected: InstructionSrcLoc?, actual: InstructionSrcLoc?) {
    if (expected == null) {
        assertNull(actual)
        return
    }

    assertNotNull(actual)
    assertDeepEquals(expected.mnemonic, actual.mnemonic)
    assertDeepEquals(expected.args, actual.args, ::assertDeepEquals)
    assertDeepEquals(expected.stackArgs, actual.stackArgs, ::assertDeepEquals)
}

fun assertDeepEquals(expected: SegmentSrcLoc?, actual: SegmentSrcLoc?) {
    if (expected == null) {
        assertNull(actual)
        return
    }

    assertNotNull(actual)
    assertDeepEquals(expected.labels, actual.labels, ::assertDeepEquals)
}
