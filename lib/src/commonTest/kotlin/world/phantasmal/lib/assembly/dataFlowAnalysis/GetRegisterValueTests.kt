package world.phantasmal.lib.assembly.dataFlowAnalysis

import world.phantasmal.lib.assembly.*
import world.phantasmal.lib.test.toInstructions
import kotlin.test.Test
import kotlin.test.assertEquals

private const val MAX_REGISTER_VALUES_SIZE: Long = 1L shl 32

class GetRegisterValueTests {
    @Test
    fun when_no_instruction_sets_the_register_zero_is_returned() {
        val im = toInstructions("""
            0:
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)
        val values = getRegisterValue(cfg, im[0].instructions[0], 6)

        assertEquals(1L, values.size)
        assertEquals(0, values[0])
    }

    @Test
    fun a_single_register_assignment_results_in_one_value() {
        val im = toInstructions("""
            0:
                leti r6, 1337
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)
        val values = getRegisterValue(cfg, im[0].instructions[1], 6)

        assertEquals(1L, values.size)
        assertEquals(1337, values[0])
    }

    @Test
    fun two_assignments_in_separate_code_paths_results_in_two_values() {
        val im = toInstructions("""
            0:
                jmp_> r1, r2, 1
                leti r10, 111
                jmp 2
            1:
                leti r10, 222
            2:
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)
        val values = getRegisterValue(cfg, im[2].instructions[0], 10)

        assertEquals(2L, values.size)
        assertEquals(111, values[0])
        assertEquals(222, values[1])
    }

    @Test
    fun bail_out_from_loops() {
        val im = toInstructions("""
            0:
                addi r10, 5
                jmpi_< r10, 500, 0
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)
        val values = getRegisterValue(cfg, im[0].instructions[2], 10)

        assertEquals(MAX_REGISTER_VALUES_SIZE, values.size)
    }

    @Test
    fun leta_and_leto() {
        val im = toInstructions("""
            0:
                leta r0, r100
                leto r1, 100
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)
        val r0 = getRegisterValue(cfg, im[0].instructions[2], 0)

        assertEquals(MAX_REGISTER_VALUES_SIZE, r0.size)
        assertEquals(MIN_REGISTER_VALUE, r0.minOrNull())
        assertEquals(MAX_REGISTER_VALUE, r0.maxOrNull())

        val r1 = getRegisterValue(cfg, im[0].instructions[2], 1)

        assertEquals(MAX_REGISTER_VALUES_SIZE, r1.size)
        assertEquals(MIN_REGISTER_VALUE, r1.minOrNull())
        assertEquals(MAX_REGISTER_VALUE, r1.maxOrNull())
    }

    @Test
    fun rev() {
        val im = toInstructions("""
            0:
                leti r0, 10
                leti r1, 50
                get_random r0, r10
                rev r10
                leti r0, -10
                leti r1, 50
                get_random r0, r10
                rev r10
                leti r10, 0
                rev r10
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)
        val v0 = getRegisterValue(cfg, im[0].instructions[4], 10)

        assertEquals(1L, v0.size)
        assertEquals(0, v0[0])

        val v1 = getRegisterValue(cfg, im[0].instructions[8], 10)

        assertEquals(2L, v1.size)
        assertEquals(0, v1[0])
        assertEquals(1, v1[1])

        val v2 = getRegisterValue(cfg, im[0].instructions[10], 10)

        assertEquals(1L, v2.size)
        assertEquals(1, v2[0])
    }

    @Test
    fun addi() {
        testBranched(OP_ADDI, 25, 35)
    }

    @Test
    fun subi() {
        testBranched(OP_SUBI, -5, 5)
    }

    @Test
    fun muli() {
        testBranched(OP_MULI, 150, 300)
    }

    @Test
    fun divi() {
        testBranched(OP_DIVI, 0, 1)
    }

    /**
     * Test an instruction taking a register and an integer.
     * The instruction will be called with arguments r99, 15. r99 will be set to 10 or 20.
     */
    private fun testBranched(opcode: Opcode, expected1: Int, expected2: Int) {
        val im = toInstructions("""
            0:
                leti r99, 10
                jmpi_= r0, 100, 1
                leti r99, 20
            1:
                ${opcode.mnemonic} r99, 15
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)
        val values = getRegisterValue(cfg, im[1].instructions[1], 99)

        assertEquals(2L, values.size)
        assertEquals(expected1, values[0])
        assertEquals(expected2, values[1])
    }

    @Test
    fun get_random() {
        val im = toInstructions("""
            0:
                leti r0, 20
                leti r1, 20
                get_random r0, r10
                leti r1, 19
                get_random r0, r10
                leti r1, 25
                get_random r0, r10
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)
        val v0 = getRegisterValue(cfg, im[0].instructions[3], 10)

        assertEquals(1L, v0.size)
        assertEquals(20, v0[0])

        val v1 = getRegisterValue(cfg, im[0].instructions[5], 10)

        assertEquals(1L, v1.size)
        assertEquals(20, v1[0])

        val v2 = getRegisterValue(cfg, im[0].instructions[7], 10)

        assertEquals(5L, v2.size)
        assertEquals(20, v2[0])
        assertEquals(21, v2[1])
        assertEquals(22, v2[2])
        assertEquals(23, v2[3])
        assertEquals(24, v2[4])
    }
}
