package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.asm.InstructionSegment
import world.phantasmal.psolib.asm.disassemble
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class InspectUnknownOpcodes : LibTestSuite {
    // Override with PSO_GAME_DATA_DIR; defaults to a path that exists on the original author's
    // machine. Test skips cleanly when the directory is absent (see early-return below).
    private val gameDataDir =
        File(System.getenv("PSO_GAME_DATA_DIR") ?: "D:/PSO/EphineaPSO2/data")

    @Test
    fun inspect() = testAsync {
        if (!gameDataDir.exists()) { println("Skipping: game data not found at $gameDataDir"); return@testAsync }
        val bins = listOf(
            "map_city_on_j.bin" to true,
            "map_labo_on_j.bin" to true,
        )
        for ((fileName, sj) in bins) {
            val buf = Buffer.fromByteArray(File(gameDataDir, fileName).readBytes())
            val bin = parseBin(buf.cursor(), shiftJis = sj)
            val ir = parseBytecode(
                bin.bytecode, bin.labelOffsets, emptySet(),
                bin.format.stringEncoding, lenient = true,
            )
            assertTrue(ir is Success)

            val lines = disassemble(ir.value)
            // Find lines with unknown_f9
            println("=== $fileName ===")
            for ((i, line) in lines.withIndex()) {
                if (line.contains("unknown_f9")) {
                    // Print context: 2 lines before and after
                    for (j in maxOf(0, i-2)..minOf(lines.lastIndex, i+2)) {
                        println("  ${j+1}: ${lines[j]}")
                    }
                    println()
                }
            }
        }
    }
}
