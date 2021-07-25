package world.phantasmal.psolib.asm

import world.phantasmal.core.Success
import world.phantasmal.psolib.fileFormats.quest.parseBin
import world.phantasmal.psolib.fileFormats.quest.parseBytecode
import world.phantasmal.psolib.fileFormats.quest.writeBytecode
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.assertDeepEquals
import world.phantasmal.psolib.test.readFile
import world.phantasmal.testUtils.assertDeepEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisassemblyAssemblyRoundTripTests : LibTestSuite {
    @Test
    fun assembling_disassembled_bytecode_should_result_in_the_same_IR() = testAsync {
        assembling_disassembled_bytecode_should_result_in_the_same_IR(inlineStackArgs = false)
    }

    @Test
    fun assembling_disassembled_bytecode_should_result_in_the_same_IR_inline_args() = testAsync {
        assembling_disassembled_bytecode_should_result_in_the_same_IR(inlineStackArgs = true)
    }

    private suspend fun assembling_disassembled_bytecode_should_result_in_the_same_IR(
        inlineStackArgs: Boolean,
    ) {
        val bin = parseBin(readFile("/quest27_e_decompressed.bin"))
        val expectedIr = parseBytecode(
            bin.bytecode,
            bin.labelOffsets,
            setOf(0),
            dcGcFormat = false,
            lenient = false,
        ).unwrap()

        val assemblyResult =
            assemble(disassemble(expectedIr, inlineStackArgs), inlineStackArgs)

        assertTrue(assemblyResult.problems.isEmpty())
        assertTrue(assemblyResult is Success)
        assertDeepEquals(expectedIr, assemblyResult.value, ignoreSrcLocs = true)
    }

    @Test
    fun disassembling_assembled_bytecode_should_result_in_the_same_ASM() = testAsync {
        disassembling_assembled_bytecode_should_result_in_the_same_ASM(inlineStackArgs = false)
    }

    @Test
    fun disassembling_assembled_bytecode_should_result_in_the_same_ASM_inline_args() = testAsync {
        disassembling_assembled_bytecode_should_result_in_the_same_ASM(inlineStackArgs = true)
    }

    private suspend fun disassembling_assembled_bytecode_should_result_in_the_same_ASM(
        inlineStackArgs: Boolean,
    ) {
        val bin = parseBin(readFile("/quest27_e_decompressed.bin"))
        val ir = parseBytecode(
            bin.bytecode,
            bin.labelOffsets,
            setOf(0),
            dcGcFormat = false,
            lenient = false,
        ).unwrap()

        val expectedAsm = disassemble(ir, inlineStackArgs)
        val actualAsm =
            disassemble(assemble(expectedAsm, inlineStackArgs).unwrap(), inlineStackArgs)

        assertDeepEquals(expectedAsm, actualAsm, ::assertEquals)
    }

    @Test
    fun assembling_disassembled_bytecode_without_inline_stack_args_results_in_the_same_bytecode() =
        testAsync {
            assembling_disassembled_bytecode_without_inline_stack_args_results_in_the_same_bytecode(
                inlineStackArgs = false
            )
        }

    @Test
    fun assembling_disassembled_bytecode_without_inline_stack_args_results_in_the_same_bytecode_inline_args() =
        testAsync {
            assembling_disassembled_bytecode_without_inline_stack_args_results_in_the_same_bytecode(
                inlineStackArgs = true
            )
        }

    /**
     * Parse and disassemble Seat of the Heart bytecode, assemble the ASM to IR and write the IR to
     * bytecode again, then check whether the original and the new bytecode are byte-for-byte equal.
     */
    private suspend fun assembling_disassembled_bytecode_without_inline_stack_args_results_in_the_same_bytecode(
        inlineStackArgs: Boolean,
    ) {
        val origBin = parseBin(readFile("/quest27_e_decompressed.bin"))
        val origBytecode = origBin.bytecode
        val result = assemble(
            disassemble(
                parseBytecode(
                    origBytecode,
                    origBin.labelOffsets,
                    setOf(0),
                    dcGcFormat = false,
                    lenient = false,
                ).unwrap(),
                inlineStackArgs,
            ), inlineStackArgs
        )

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        val newBytecode = writeBytecode(result.value, dcGcFormat = false).bytecode

        assertDeepEquals(origBytecode, newBytecode)
    }
}
