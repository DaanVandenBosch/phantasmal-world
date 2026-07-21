package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.asm.InstructionSegment
import world.phantasmal.psolib.asm.codeToOpcode
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Mirrors the web-side `analyzeDataLabels` back-trace logic for
 * `set_symbol_chat_collision` (0xF8A6) and asserts it correctly identifies
 * the dlabel references in `symbol_chat_test.qst`.
 *
 * Run with: ./gradlew :psolib:jvmTest --tests "*.SymbolChatAnalysisTest"
 */
class SymbolChatAnalysisTest : LibTestSuite {

    private val opcodeLeti = codeToOpcode(0x09)
    private val opcodeF8A6 = codeToOpcode(0xF8A6)

    /**
     * Mirrors DataLabelAnalysis.kt — scans for `set_symbol_chat_collision`,
     * back-traces `leti` writes for R+7..R+9, returns the set of dlabel ids.
     */
    private fun detectSymbolChatHexLabels(quest: Quest): Set<Int> {
        val labels = mutableSetOf<Int>()
        for (seg in quest.bytecodeIr.segments) {
            if (seg !is InstructionSegment) continue
            seg.instructions.forEachIndexed { idx, inst ->
                if (inst.opcode !== opcodeF8A6) return@forEachIndexed
                val baseReg = inst.args.getOrNull(0)?.coerceInt() ?: return@forEachIndexed
                for (slot in 7..9) {
                    val targetReg = baseReg + slot
                    for (j in (idx - 1) downTo 0) {
                        val prev = seg.instructions[j]
                        if (prev.opcode !== opcodeLeti) continue
                        val writeReg = prev.args.getOrNull(0)?.coerceInt() ?: continue
                        if (writeReg != targetReg) continue
                        prev.args.getOrNull(1)?.coerceInt()?.let { labels.add(it) }
                        break
                    }
                }
            }
        }
        return labels
    }

    private fun loadTestQuest(): Quest {
        val projectRoot = File(System.getProperty("user.dir")).let {
            if (it.name == "psolib") it.parentFile else it
        }
        val file = File(projectRoot, "psolib/src/commonTest/resources/symbol_chat_test.qst")
        assertTrue(file.exists(), "Test quest not found: ${file.absolutePath}")
        val result = parseQstToQuest(Buffer.fromByteArray(file.readBytes()).cursor(), lenient = true)
        assertTrue(result is Success, "Parse failed: ${result.problems}")
        return result.value.quest
    }

    @Test
    fun detects_trigger1_labels() = testAsync {
        val quest = loadTestQuest()
        val detected = detectSymbolChatHexLabels(quest)

        // Trigger #1 (baseReg=50): R+7=9000, R+8=9024, R+9=9025
        assertTrue(9000 in detected, "Trigger #1 R+7: label 9000 not detected")
        assertTrue(9024 in detected, "Trigger #1 R+8: label 9024 not detected")
        assertTrue(9025 in detected, "Trigger #1 R+9: label 9025 not detected")
    }

    @Test
    fun detects_trigger2_labels() = testAsync {
        val quest = loadTestQuest()
        val detected = detectSymbolChatHexLabels(quest)

        // Trigger #2 (baseReg=70): R+7=9028, R+8=9031, R+9=9026
        assertTrue(9028 in detected, "Trigger #2 R+7: label 9028 not detected")
        assertTrue(9031 in detected, "Trigger #2 R+8: label 9031 not detected")
        assertTrue(9026 in detected, "Trigger #2 R+9: label 9026 not detected")
    }

    @Test
    fun detects_exactly_6_labels() = testAsync {
        val quest = loadTestQuest()
        val detected = detectSymbolChatHexLabels(quest)
        val expected = setOf(9000, 9024, 9025, 9028, 9031, 9026)
        assertEquals(expected, detected, "Unexpected labels detected")
    }

    @Test
    fun no_false_positives_from_non_referenced_labels() = testAsync {
        val quest = loadTestQuest()
        val detected = detectSymbolChatHexLabels(quest)

        // These labels exist as DataSegments but are NOT referenced by
        // any set_symbol_chat_collision call, so must NOT appear.
        val notReferenced = listOf(9001, 9002, 9010, 9023, 9027, 9029, 9030)
        for (label in notReferenced) {
            assertTrue(label !in detected, "False positive: label $label should not be detected")
        }
    }
}
