package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.asm.InstructionSegment
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.readFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * One-off inspection: load a .qst and dump every `set_symbol_chat_collision`
 * (0xF8A6) call site, plus the preceding `leti` writes that initialise its
 * 10-register block, so we can confirm static analysis will pick the dlabels
 * up correctly.
 *
 * Run with: ./gradlew :psolib:jvmTest --tests "*.InspectSymbolChatQst"
 */
class InspectSymbolChatQst : LibTestSuite {

    @Test
    fun scanAllQuests() = testAsync {
        // Bulk scan against any large local quest archives the developer happens to have.
        // None of these paths are required — the test gracefully reports zero hits when
        // the directories are absent (e.g. on CI or another developer's machine).
        val roots = listOfNotNull(
            System.getenv("PSO_QUEST_DIR")?.let(::File),
        )
        val hits = mutableListOf<Pair<File, Int>>()
        var total = 0; var parsed = 0; var failed = 0
        for (root in roots) {
            if (!root.exists()) continue
            root.walkTopDown().filter { it.isFile && it.name.endsWith(".qst", ignoreCase = true) }.forEach { f ->
                total++
                try {
                    val buf = Buffer.fromByteArray(f.readBytes())
                    val res = parseQstToQuest(buf.cursor(), lenient = true)
                    if (res !is Success) { failed++; return@forEach }
                    parsed++
                    val q = res.value.quest
                    var n = 0
                    for (seg in q.bytecodeIr.segments) {
                        if (seg !is InstructionSegment) continue
                        for (inst in seg.instructions) if (inst.opcode.code == 0xF8A6) n++
                    }
                    if (n > 0) hits.add(f to n)
                } catch (_: Throwable) { failed++ }
            }
        }
        println()
        println("=== Scan ===")
        println("Total .qst files: $total  (parsed=$parsed, failed=$failed)")
        println("Quests using set_symbol_chat_collision (0xF8A6): ${hits.size}")
        hits.sortedByDescending { it.second }.forEach { (f, n) ->
            println("  $n  ${f.absolutePath}")
        }
    }

    @Test
    fun inspectGeneratedTestQst() = testAsync {
        val res = parseQstToQuest(readFile("/symbol_chat_test.qst"), lenient = true)
        assertTrue(res is Success)
        val q = res.value.quest

        println("=== ${q.name} ===")
        println("Segments: ${q.bytecodeIr.segments.size}")

        // Walk to find the trigger segment.
        for (seg in q.bytecodeIr.segments) {
            if (seg !is InstructionSegment) continue
            if (seg.instructions.none { it.opcode.code == 0xF8A6 }) continue
            println()
            println("Trigger segment, labels=${seg.labels}, ${seg.instructions.size} insts:")
            for (inst in seg.instructions) {
                val argDesc = inst.args.joinToString(", ") {
                    if (it is world.phantasmal.psolib.asm.IntArg) {
                        if (it.isRegRef) "R${it.value}" else "${it.value}"
                    } else it.toString()
                }
                println("  ${inst.opcode.mnemonic}  $argDesc")
            }
        }

        // Now mirror the web-side analyzer's back-trace logic and dump
        // which dlabels we'd auto-detect as SymbolChatHexData.
        val opcodeLeti = world.phantasmal.psolib.asm.codeToOpcode(0x09)
        val opcodeF8A6 = world.phantasmal.psolib.asm.codeToOpcode(0xF8A6)
        val detected = mutableSetOf<Int>()
        for (seg in q.bytecodeIr.segments) {
            if (seg !is InstructionSegment) continue
            seg.instructions.forEachIndexed { idx, inst ->
                if (inst.opcode !== opcodeF8A6) return@forEachIndexed
                val baseReg = inst.args.getOrNull(0)?.coerceInt() ?: return@forEachIndexed
                println("  >> found set_symbol_chat_collision at idx=$idx, baseReg=$baseReg")
                for (slot in 7..9) {
                    val targetReg = baseReg + slot
                    var found = false
                    for (j in (idx - 1) downTo 0) {
                        val prev = seg.instructions[j]
                        if (prev.opcode !== opcodeLeti) continue
                        val writeReg = prev.args.getOrNull(0)?.coerceInt() ?: continue
                        if (writeReg != targetReg) continue
                        val labelId = prev.args.getOrNull(1)?.coerceInt() ?: break
                        println("     slot $slot: leti R$targetReg, $labelId")
                        detected.add(labelId)
                        found = true
                        break
                    }
                    if (!found) println("     slot $slot: NOT FOUND for R$targetReg")
                }
            }
        }
        println("Auto-detect result: $detected")
    }

    @Test
    fun inspectFull() = testAsync {
        val res = parseQstToQuest(readFile("/quest143_e.qst"), lenient = true)
        assertTrue(res is Success)
        val q = res.value.quest

        println("=== ${q.name}  id=${q.id}  ep=${q.episode} ===")

        // 1. SymbolChatObject entities (object type 0x21 = SymbolChatObject).
        val scObjects = q.objects.filter { it.type == ObjectType.SymbolChatObject }
        println("SymbolChatObject (type 0x21) entities: ${scObjects.size}")
        for (o in scObjects.take(20)) {
            println("  area=${o.areaId}  section=${o.sectionId}  pos=(${o.position.x}, ${o.position.y}, ${o.position.z})")
        }

        // 2. Every opcode F8xx that touches symbol chat / chat / sound.
        val codeCounts = mutableMapOf<Int, Int>()
        for (seg in q.bytecodeIr.segments) {
            if (seg !is InstructionSegment) continue
            for (inst in seg.instructions) {
                val c = inst.opcode.code
                if (c in 0xF800..0xF9FF) codeCounts.merge(c, 1) { a, b -> a + b }
            }
        }
        println()
        println("F8xx/F9xx opcodes that mention 'chat' or 'symbol' in mnemonic:")
        codeCounts.entries
            .filter { (c, _) ->
                val op = world.phantasmal.psolib.asm.codeToOpcode(c)
                op.mnemonic.contains("chat", ignoreCase = true) ||
                    op.mnemonic.contains("symbol", ignoreCase = true)
            }
            .sortedBy { it.key }
            .forEach { (c, n) ->
                val op = world.phantasmal.psolib.asm.codeToOpcode(c)
                println("  0x${c.toString(16)}  ${op.mnemonic}  ×$n")
            }
    }

    @Test
    fun inspect() = testAsync {
        val parseResult = parseQstToQuest(readFile("/quest143_e.qst"), lenient = true)
        assertTrue(parseResult is Success, "parse failed: ${parseResult.problems}")
        val quest = parseResult.value.quest

        println("=== Quest: ${quest.name} (id=${quest.id}, ep=${quest.episode}) ===")
        println("Segments: ${quest.bytecodeIr.segments.size}")

        var totalCalls = 0
        for ((segIdx, segment) in quest.bytecodeIr.segments.withIndex()) {
            if (segment !is InstructionSegment) continue
            segment.instructions.forEachIndexed { idx, inst ->
                if (inst.opcode.code == 0xF8A6) {
                    totalCalls++
                    val baseReg = inst.args.getOrNull(0)?.coerceInt()
                    println()
                    println("[$totalCalls] segment#$segIdx (labels=${segment.labels}) inst#$idx")
                    println("    set_symbol_chat_collision  baseReg=R$baseReg")

                    // Walk backward in this segment for leti writes to baseReg+0..+9.
                    if (baseReg != null) {
                        val slotValues = arrayOfNulls<Int>(10)
                        for (j in (idx - 1) downTo 0) {
                            val prev = segment.instructions[j]
                            if (prev.opcode.code != 0x09) continue // leti
                            val reg = prev.args.getOrNull(0)?.coerceInt() ?: continue
                            val slot = reg - baseReg
                            if (slot in 0..9 && slotValues[slot] == null) {
                                slotValues[slot] = prev.args.getOrNull(1)?.coerceInt()
                            }
                            if (slotValues.all { it != null }) break
                        }
                        val labels = listOf(
                            "X     " to slotValues[0]?.let { java.lang.Float.intBitsToFloat(it).toString() },
                            "Y     " to slotValues[1]?.let { java.lang.Float.intBitsToFloat(it).toString() },
                            "Z     " to slotValues[2]?.let { java.lang.Float.intBitsToFloat(it).toString() },
                            "Radius" to slotValues[3]?.let { java.lang.Float.intBitsToFloat(it).toString() },
                            "Lock1 " to slotValues[4]?.let { "0x%08x (id=%d)".format(it, (it ushr 16) and 0xFFFF) },
                            "Lock2 " to slotValues[5]?.let { "0x%08x (id=%d)".format(it, (it ushr 16) and 0xFFFF) },
                            "Lock3 " to slotValues[6]?.let { "0x%08x (id=%d)".format(it, (it ushr 16) and 0xFFFF) },
                            "SC1   " to slotValues[7]?.let { "label $it" },
                            "SC2   " to slotValues[8]?.let { "label $it" },
                            "SC3   " to slotValues[9]?.let { "label $it" },
                        )
                        for ((name, v) in labels) {
                            println("      R+${labels.indexOfFirst { it.first == name }} $name = ${v ?: "<not statically resolved>"}")
                        }
                    }
                }
            }
        }
        println()
        println("Total set_symbol_chat_collision calls: $totalCalls")
        println("Total data labels referenced (auto-detect candidates):")
        val detected = run {
            // Mirror the analyzer's logic at byte level for sanity.
            val labels = mutableSetOf<Int>()
            for (segment in quest.bytecodeIr.segments) {
                if (segment !is InstructionSegment) continue
                segment.instructions.forEachIndexed { idx, inst ->
                    if (inst.opcode.code != 0xF8A6) return@forEachIndexed
                    val baseReg = inst.args.getOrNull(0)?.coerceInt() ?: return@forEachIndexed
                    for (slot in 7..9) {
                        val target = baseReg + slot
                        for (j in (idx - 1) downTo 0) {
                            val prev = segment.instructions[j]
                            if (prev.opcode.code != 0x09) continue
                            val r = prev.args.getOrNull(0)?.coerceInt() ?: continue
                            if (r != target) continue
                            prev.args.getOrNull(1)?.coerceInt()?.let { labels.add(it) }
                            break
                        }
                    }
                }
            }
            labels.sorted()
        }
        println("  $detected")
    }
}
