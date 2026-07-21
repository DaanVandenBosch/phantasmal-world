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
        val bin = parseBin(readFile("/seat_of_the_heart_decompressed.bin"))
        val expectedIr = parseBytecode(
            bin.bytecode,
            bin.labelOffsets,
            setOf(0),
            stringEncoding = BytecodeStringEncoding.UTF16,
            lenient = false,
        ).unwrap()

        val assemblyResult = assemble(disassemble(expectedIr))

        assertTrue(assemblyResult.problems.isEmpty())
        assertTrue(assemblyResult is Success)
        assertDeepEquals(expectedIr, assemblyResult.value, ignoreSrcLocs = true)
    }

    @Test
    fun disassembling_assembled_bytecode_should_result_in_the_same_ASM() = testAsync {
        val bin = parseBin(readFile("/seat_of_the_heart_decompressed.bin"))
        val ir = parseBytecode(
            bin.bytecode,
            bin.labelOffsets,
            setOf(0),
            stringEncoding = BytecodeStringEncoding.UTF16,
            lenient = false,
        ).unwrap()

        val expectedAsm = disassemble(ir)
        val actualAsm = disassemble(assemble(expectedAsm).unwrap())

        assertDeepEquals(expectedAsm, actualAsm, ::assertEquals)
    }

    @Test
    fun assembling_disassembled_bytecode_results_in_the_same_bytecode() =
        testAsync {
            val origBin = parseBin(readFile("/seat_of_the_heart_decompressed.bin"))
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
                )
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
     * GC (ASCII) disassemble -> assemble -> write round-trip.
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
            )
        )

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        val newBytecode = writeBytecode(result.value, BytecodeStringEncoding.ASCII).bytecode

        assertDeepEquals(origBytecode, newBytecode)
    }
}
