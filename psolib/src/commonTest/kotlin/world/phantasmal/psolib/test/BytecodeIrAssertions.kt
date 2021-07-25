package world.phantasmal.psolib.test

import world.phantasmal.psolib.asm.*
import world.phantasmal.testUtils.assertDeepEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

fun assertDeepEquals(
    expected: BytecodeIr,
    actual: BytecodeIr,
    ignoreSrcLocs: Boolean = false,
    message: String? = null,
) {
    assertDeepEquals(
        expected.segments,
        actual.segments,
        { a, b, m -> assertDeepEquals(a, b, ignoreSrcLocs, m) },
        message,
    )
}

fun assertDeepEquals(
    expected: Segment,
    actual: Segment,
    ignoreSrcLocs: Boolean = false,
    message: String? = null,
) {
    assertEquals(expected::class, actual::class, message)
    assertDeepEquals(expected.labels, actual.labels, ::assertEquals, message)

    if (!ignoreSrcLocs) {
        assertDeepEquals(expected.srcLoc, actual.srcLoc, message)
    }

    when (expected) {
        is InstructionSegment -> {
            actual as InstructionSegment
            assertDeepEquals(
                expected.instructions,
                actual.instructions,
                { a, b, m -> assertDeepEquals(a, b, ignoreSrcLocs, m) },
                message,
            )
        }
        is DataSegment -> {
            actual as DataSegment
            assertDeepEquals(expected.data, actual.data, message)
        }
        is StringSegment -> {
            actual as StringSegment
            assertEquals(expected.value, actual.value, message)
        }
    }
}

fun assertDeepEquals(
    expected: Instruction,
    actual: Instruction,
    ignoreSrcLocs: Boolean = false,
    message: String? = null,
) {
    assertEquals(expected.opcode, actual.opcode, message)
    assertEquals(expected.valid, actual.valid, message)
    assertDeepEquals(expected.args, actual.args, ::assertEquals, message)

    if (!ignoreSrcLocs) {
        assertDeepEquals(expected.srcLoc, actual.srcLoc, message)
    }
}

fun assertDeepEquals(expected: SrcLoc?, actual: SrcLoc?, message: String? = null) {
    if (expected == null) {
        assertNull(actual, message)
        return
    }

    assertNotNull(actual, message)
    assertEquals(expected.lineNo, actual.lineNo, message)
    assertEquals(expected.col, actual.col, message)
    assertEquals(expected.len, actual.len, message)
}

fun assertDeepEquals(
    expected: InstructionSrcLoc?,
    actual: InstructionSrcLoc?,
    message: String? = null,
) {
    if (expected == null) {
        assertNull(actual, message)
        return
    }

    assertNotNull(actual, message)
    assertEquals(expected.trailingArgSeparator, actual.trailingArgSeparator, message)
    assertDeepEquals(expected.mnemonic, actual.mnemonic, message)
    assertDeepEquals(expected.args, actual.args, ::assertDeepEquals, message)
}

fun assertDeepEquals(expected: ArgSrcLoc?, actual: ArgSrcLoc?, message: String? = null) {
    if (expected == null) {
        assertNull(actual, message)
        return
    }

    assertNotNull(actual, message)
    assertDeepEquals(expected.precise, actual.precise, message)
    assertDeepEquals(expected.coarse, actual.coarse, message)
}

fun assertDeepEquals(expected: SegmentSrcLoc?, actual: SegmentSrcLoc?, message: String? = null) {
    if (expected == null) {
        assertNull(actual, message)
        return
    }

    assertNotNull(actual, message)
    assertDeepEquals(expected.labels, actual.labels, ::assertDeepEquals, message)
}
