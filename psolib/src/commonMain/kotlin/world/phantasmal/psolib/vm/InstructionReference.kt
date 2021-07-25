package world.phantasmal.psolib.vm

import world.phantasmal.psolib.asm.BytecodeIr
import world.phantasmal.psolib.asm.Instruction
import world.phantasmal.psolib.asm.InstructionSegment

class InstructionReference(
    val segIdx: Int,
    val instIdx: Int,
    private val bytecode: BytecodeIr,
) {
    val segment: InstructionSegment = bytecode.segments[segIdx] as InstructionSegment
    val instruction: Instruction = segment.instructions[instIdx]

    /**
     * Returns a reference to the next instruction.
     */
    fun next(): InstructionReference? {
        if (instIdx < segment.instructions.lastIndex) {
            return InstructionReference(segIdx, instIdx + 1, bytecode)
        }

        // Segment ended, move to the next segment.
        if (segIdx < bytecode.segments.lastIndex &&
            bytecode.segments[segIdx + 1] is InstructionSegment
        ) {
            return InstructionReference(segIdx + 1, 0, bytecode)
        }

        // Reached a non-instruction segment or EOF.
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as InstructionReference
        return segIdx == other.segIdx && instIdx == other.instIdx
    }

    override fun hashCode(): Int = 31 * segIdx + instIdx
}
