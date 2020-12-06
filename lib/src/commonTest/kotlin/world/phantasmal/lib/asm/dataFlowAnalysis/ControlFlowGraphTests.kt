package world.phantasmal.lib.asm.dataFlowAnalysis

import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.toInstructions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlFlowGraphTests : LibTestSuite() {
    @Test
    fun single_instruction() {
        val im = toInstructions("""
            0:
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)

        assertEquals(1, cfg.blocks.size)
        assertEquals(0, cfg.blocks[0].start)
        assertEquals(1, cfg.blocks[0].end)
        assertEquals(BranchType.Return, cfg.blocks[0].branchType)
        assertTrue(cfg.blocks[0].from.isEmpty())
        assertTrue(cfg.blocks[0].to.isEmpty())
        assertTrue(cfg.blocks[0].branchLabels.isEmpty())
    }

    @Test
    fun single_unconditional_jump() {
        val im = toInstructions("""
            0:
                jmp 1
            1:
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)

        assertEquals(2, cfg.blocks.size)

        assertEquals(0, cfg.blocks[0].start)
        assertEquals(1, cfg.blocks[0].end)
        assertEquals(BranchType.Jump, cfg.blocks[0].branchType)
        assertEquals(0, cfg.blocks[0].from.size)
        assertEquals(1, cfg.blocks[0].to.size)
        assertEquals(1, cfg.blocks[0].branchLabels.size)

        assertEquals(0, cfg.blocks[1].start)
        assertEquals(1, cfg.blocks[1].end)
        assertEquals(BranchType.Return, cfg.blocks[1].branchType)
        assertEquals(1, cfg.blocks[1].from.size)
        assertEquals(0, cfg.blocks[1].to.size)
        assertEquals(0, cfg.blocks[1].branchLabels.size)
    }

    @Test
    fun single_conditional_jump() {
        val im = toInstructions("""
            0:
                jmp_= r1, r2, 1
                ret
            1:
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)

        assertEquals(3, cfg.blocks.size)

        assertEquals(0, cfg.blocks[0].start)
        assertEquals(1, cfg.blocks[0].end)
        assertEquals(BranchType.ConditionalJump, cfg.blocks[0].branchType)
        assertEquals(0, cfg.blocks[0].from.size)
        assertEquals(2, cfg.blocks[0].to.size)
        assertEquals(1, cfg.blocks[0].branchLabels.size)

        assertEquals(1, cfg.blocks[1].start)
        assertEquals(2, cfg.blocks[1].end)
        assertEquals(BranchType.Return, cfg.blocks[1].branchType)
        assertEquals(1, cfg.blocks[1].from.size)
        assertEquals(0, cfg.blocks[1].to.size)
        assertEquals(0, cfg.blocks[1].branchLabels.size)

        assertEquals(0, cfg.blocks[2].start)
        assertEquals(1, cfg.blocks[2].end)
        assertEquals(BranchType.Return, cfg.blocks[2].branchType)
        assertEquals(1, cfg.blocks[2].from.size)
        assertEquals(0, cfg.blocks[2].to.size)
        assertEquals(0, cfg.blocks[2].branchLabels.size)
    }

    @Test
    fun single_call() {
        val im = toInstructions("""
            0:
                call 1
                ret
            1:
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)

        assertEquals(3, cfg.blocks.size)

        assertEquals(0, cfg.blocks[0].start)
        assertEquals(1, cfg.blocks[0].end)
        assertEquals(BranchType.Call, cfg.blocks[0].branchType)
        assertEquals(0, cfg.blocks[0].from.size)
        assertEquals(1, cfg.blocks[0].to.size)
        assertEquals(1, cfg.blocks[0].branchLabels.size)

        assertEquals(1, cfg.blocks[1].start)
        assertEquals(2, cfg.blocks[1].end)
        assertEquals(BranchType.Return, cfg.blocks[1].branchType)
        assertEquals(1, cfg.blocks[1].from.size)
        assertEquals(0, cfg.blocks[1].to.size)
        assertEquals(0, cfg.blocks[1].branchLabels.size)

        assertEquals(0, cfg.blocks[2].start)
        assertEquals(1, cfg.blocks[2].end)
        assertEquals(BranchType.Return, cfg.blocks[2].branchType)
        assertEquals(1, cfg.blocks[2].from.size)
        assertEquals(1, cfg.blocks[2].to.size)
        assertEquals(0, cfg.blocks[2].branchLabels.size)
    }

    @Test
    fun conditional_jump_with_fall_through() {
        val im = toInstructions("""
            0:
                jmp_> r1, r2, 1
                nop
            1:
                nop
                ret
        """.trimIndent())
        val cfg = ControlFlowGraph.create(im)

        assertEquals(3, cfg.blocks.size)

        assertEquals(0, cfg.blocks[0].start)
        assertEquals(1, cfg.blocks[0].end)
        assertEquals(BranchType.ConditionalJump, cfg.blocks[0].branchType)
        assertEquals(0, cfg.blocks[0].from.size)
        assertEquals(2, cfg.blocks[0].to.size)
        assertEquals(1, cfg.blocks[0].branchLabels.size)

        assertEquals(1, cfg.blocks[1].start)
        assertEquals(2, cfg.blocks[1].end)
        assertEquals(BranchType.None, cfg.blocks[1].branchType)
        assertEquals(1, cfg.blocks[1].from.size)
        assertEquals(1, cfg.blocks[1].to.size)
        assertEquals(0, cfg.blocks[1].branchLabels.size)

        assertEquals(0, cfg.blocks[2].start)
        assertEquals(2, cfg.blocks[2].end)
        assertEquals(BranchType.Return, cfg.blocks[2].branchType)
        assertEquals(2, cfg.blocks[2].from.size)
        assertEquals(0, cfg.blocks[2].to.size)
        assertEquals(0, cfg.blocks[2].branchLabels.size)
    }
}
