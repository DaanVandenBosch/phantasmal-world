package world.phantasmal.psolib.asm

import world.phantasmal.core.Success
import world.phantasmal.psolib.compression.prs.prsDecompress
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.quest.*
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
            stringEncoding = BytecodeStringEncoding.UTF16,
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
            stringEncoding = BytecodeStringEncoding.UTF16,
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
                    stringEncoding = BytecodeStringEncoding.UTF16,
                    lenient = false,
                ).unwrap(),
                inlineStackArgs,
            ), inlineStackArgs
        )

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        val newBytecode = writeBytecode(result.value, stringEncoding = BytecodeStringEncoding.UTF16).bytecode

        assertDeepEquals(origBytecode, newBytecode)
    }

    /**
     * Byte-for-byte round-trip for a GC (ASCII encoding) quest extracted from a .qst file.
     * Covers the ASCII code path that the BB tests above don't exercise.
     */
    @Test
    fun gc_quest_bytecode_round_trip() = testAsync {
        val qst = parseQst(readFile("/lost_heat_sword_gc.qst")).unwrap()
        val binFile = qst.files.first { it.filename.trim().lowercase().endsWith(".bin") }
        val origBin = parseBin(prsDecompress(binFile.data.cursor()).unwrap())
        assertEquals(BinFormat.DC_GC, origBin.format)

        val origBytecode = origBin.bytecode

        val ir = parseBytecode(
            origBytecode,
            origBin.labelOffsets,
            setOf(0),
            stringEncoding = BytecodeStringEncoding.ASCII,
            lenient = false,
        ).unwrap()

        val newBytecode = writeBytecode(ir, BytecodeStringEncoding.ASCII).bytecode

        assertDeepEquals(origBytecode, newBytecode)
    }

    /**
     * GC (ASCII) disassemble → assemble → write round-trip.
     */
    @Test
    fun gc_quest_disassemble_assemble_bytecode_round_trip() = testAsync {
        val qst = parseQst(readFile("/lost_heat_sword_gc.qst")).unwrap()
        val binFile = qst.files.first { it.filename.trim().lowercase().endsWith(".bin") }
        val origBin = parseBin(prsDecompress(binFile.data.cursor()).unwrap())

        val origBytecode = origBin.bytecode

        val result = assemble(
            disassemble(
                parseBytecode(
                    origBytecode,
                    origBin.labelOffsets,
                    setOf(0),
                    stringEncoding = BytecodeStringEncoding.ASCII,
                    lenient = false,
                ).unwrap(),
                true,
            ), true
        )

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        val newBytecode = writeBytecode(result.value, BytecodeStringEncoding.ASCII).bytecode

        assertDeepEquals(origBytecode, newBytecode)
    }
}
