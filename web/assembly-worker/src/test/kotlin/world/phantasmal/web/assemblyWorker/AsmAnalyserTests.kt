package world.phantasmal.web.assemblyWorker

import world.phantasmal.web.assemblyWorker.test.AssemblyWorkerTestSuite
import world.phantasmal.web.assemblyWorker.test.assertDeepEquals
import world.phantasmal.web.shared.messages.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsmAnalyserTests : AssemblyWorkerTestSuite() {
    @Test
    fun getSignatureHelp() = test {
        val analyser = createAsmAnalyser(
            """
            .code
            0: leti r255, 1337
            """.trimIndent()
        )

        val requestId = 113

        for (col in 1..3) {
            val response = analyser.getSignatureHelp(requestId, lineNo = 2, col)

            assertDeepEquals(Response.GetSignatureHelp(requestId, null), response)
        }

        fun sigHelp(activeParameter: Int) = Response.GetSignatureHelp(
            requestId,
            SignatureHelp(
                Signature(
                    label = "leti Reg<out Int>, Int",
                    documentation = "Sets a register to the given value.",
                    listOf(
                        Parameter(labelStart = 5, labelEnd = 17, null),
                        Parameter(labelStart = 19, labelEnd = 22, null),
                    ),
                ),
                activeParameter,
            ),
        )

        for ((colRange, sigHelp) in listOf(
            4..7 to sigHelp(-1),
            8..13 to sigHelp(0),
            14..19 to sigHelp(1),
        )) {
            for (col in colRange) {
                val response = analyser.getSignatureHelp(requestId, 2, col)

                assertDeepEquals(sigHelp, response, "col = $col")
            }
        }
    }

    @Test
    fun getSignatureHelp_incomplete_instruction() = test {
        // Three kinds of incomplete instructions.
        val analyser = createAsmAnalyser(
            """
            .code
            0:
                leti
                leti r0
                leti r0,
            """.trimIndent()
        )

        val requestId = 113

        for (col in 1..4) {
            val response = analyser.getSignatureHelp(requestId, lineNo = 3, col)

            assertDeepEquals(Response.GetSignatureHelp(requestId, null), response)
        }

        fun sigHelp(activeParameter: Int) = Response.GetSignatureHelp(
            requestId,
            SignatureHelp(
                Signature(
                    label = "leti Reg<out Int>, Int",
                    documentation = "Sets a register to the given value.",
                    listOf(
                        Parameter(labelStart = 5, labelEnd = 17, null),
                        Parameter(labelStart = 19, labelEnd = 22, null),
                    ),
                ),
                activeParameter,
            ),
        )

        // First instruction: leti
        for ((colRange, sigHelp) in listOf(
            5..8 to sigHelp(-1),
            9..9 to sigHelp(0),
        )) {
            for (col in colRange) {
                val response = analyser.getSignatureHelp(requestId, 3, col)

                assertDeepEquals(sigHelp, response, "col = $col")
            }
        }

        // Second instruction: leti r0
        for ((colRange, sigHelp) in listOf(
            5..8 to sigHelp(-1),
            9..12 to sigHelp(0),
        )) {
            for (col in colRange) {
                val response = analyser.getSignatureHelp(requestId, 4, col)

                assertDeepEquals(sigHelp, response, "col = $col")
            }
        }

        // Third instruction: leti r0,
        for ((colRange, sigHelp) in listOf(
            5..8 to sigHelp(-1),
            9..12 to sigHelp(0),
            13..13 to sigHelp(1),
        )) {
            for (col in colRange) {
                val response = analyser.getSignatureHelp(requestId, 5, col)

                assertDeepEquals(sigHelp, response, "col = $col")
            }
        }
    }

    @Test
    fun getHighlights_for_instruction() = test {
        val analyser = createAsmAnalyser(
            """
            .code
            0:
                ret
                ret
                ret
            """.trimIndent()
        )

        val requestId = 223

        // Center char "e" of center ret instruction.
        val response = analyser.getHighlights(requestId, 4, 6)

        assertEquals(3, response.result.size)
        assertEquals(AsmRange(3, 5, 3, 8), response.result[0])
        assertEquals(AsmRange(4, 5, 4, 8), response.result[1])
        assertEquals(AsmRange(5, 5, 5, 8), response.result[2])
    }

    @Test
    fun getHighlights_for_int() = test {
        val analyser = createAsmAnalyser(
            """
            .code
            0:
                set_episode 0
                set_episode 0
                set_episode 0
            """.trimIndent()
        )

        val requestId = 137

        // 0 Argument of center set_episode instruction.
        val response = analyser.getHighlights(requestId, 4, 17)

        assertTrue(response.result.isEmpty())
    }

    @Test
    fun getHighlights_col_right_after_mnemonic() = test {
        val analyser = createAsmAnalyser(
            """
            .code
            0:
                leti r10, 4000
                leti r10, 4000
                leti r10, 4000
            """.trimIndent()
        )

        val requestId = 2999

        // Cursor is right after the center leti instruction.
        val response = analyser.getHighlights(requestId, 4, 9)

        assertEquals(3, response.result.size)
        assertEquals(AsmRange(3, 5, 3, 9), response.result[0])
        assertEquals(AsmRange(4, 5, 4, 9), response.result[1])
        assertEquals(AsmRange(5, 5, 5, 9), response.result[2])
    }

    @Test
    fun getHighlights_for_label() = test {
        // The following ASM contains, in the given order:
        // - Label 100
        // - Reference to label 100 by arg_pushl instruction
        // - Reference to label 100 by arg_pushr instruction (should not be highlighted, the value
        //   comes from a register and might be used for many things)
        // - Immediate argument referencing label 100
        val analyser = createAsmAnalyser(
            """
            .code
            100:
                set_floor_handler 0, 100
                leti r0, 100
                set_floor_handler 0, r0
                jmp 100
            """.trimIndent()
        )

        val requestId = 2999

        for ((lineNo, col) in listOf(
            // Cursor is at center of label 100.
            Pair(2, 2),
            // Cursor is at center of the first set_floor_handler label argument.
            Pair(3, 27),
            // Cursor is at center of the jmp label argument.
            Pair(6, 10),
        )) {
            val response = analyser.getHighlights(requestId, lineNo, col)

            assertEquals(requestId, response.id)
            assertEquals(3, response.result.size)
            assertEquals(AsmRange(2, 1, 2, 4), response.result[0])
            assertEquals(AsmRange(3, 26, 3, 29), response.result[1])
            assertEquals(AsmRange(6, 9, 6, 12), response.result[2])
        }
    }

    private fun createAsmAnalyser(asm: String): AsmAnalyser {
        val analyser = AsmAnalyser()
        analyser.setAsm(asm.split("\n"), inlineStackArgs = true)
        analyser.processAsm()
        return analyser
    }
}
