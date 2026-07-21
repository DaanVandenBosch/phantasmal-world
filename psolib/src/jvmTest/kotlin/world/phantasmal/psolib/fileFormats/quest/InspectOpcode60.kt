package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.asm.InstructionSegment
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.test.LibTestSuite
import java.io.File
import kotlin.test.Test

/**
 * One-off probe: disassemble newserv's quest dumps and look at how 0x60 (npc_crt)
 * and 0xF94D are actually used. We're checking whether they take inline register
 * operands (yml's current model) or pop pushed args from the stack (newserv's model).
 */
class InspectOpcode60 : LibTestSuite {
    private val dumpDir = File("C:/Users/wangz/IdeaProjects/newserv/tools/quest_dumps_ephinea")

    @Test
    fun scan() = testAsync {
        if (!dumpDir.exists()) { println("Skipping: $dumpDir absent"); return@testAsync }
        val targets = setOf(0x60, 0xF94D)
        var totalQst = 0
        var hits60 = 0
        var hitsF94D = 0
        val examples = mutableListOf<String>()

        dumpDir.walkTopDown().filter { it.isFile && it.name.endsWith(".qst") }.forEach { f ->
            totalQst++
            val res = try {
                parseQstToQuest(Buffer.fromByteArray(f.readBytes()).cursor(), lenient = true)
            } catch (_: Throwable) { return@forEach }
            if (res !is Success) return@forEach
            val q = res.value.quest
            for (seg in q.bytecodeIr.segments) {
                if (seg !is InstructionSegment) continue
                val insns = seg.instructions
                insns.forEachIndexed { idx, inst ->
                    val code = inst.opcode.code
                    if (code !in targets) return@forEachIndexed
                    if (code == 0x60) hits60++ else hitsF94D++
                    if (examples.size >= 8) return@forEachIndexed
                    val window = (idx - 3).coerceAtLeast(0)..(idx + 1).coerceAtMost(insns.lastIndex)
                    val ctx = window.joinToString(" | ") { i ->
                        val ii = insns[i]
                        val a = ii.args.joinToString(",") { arg ->
                            when (arg) {
                                is world.phantasmal.psolib.asm.IntArg ->
                                    if (arg.isRegRef) "R${arg.value}" else arg.value.toString()
                                else -> arg.toString()
                            }
                        }
                        "${ii.opcode.mnemonic}($a)"
                    }
                    examples += "${f.name}: $ctx"
                }
            }
        }
        println("Scanned $totalQst .qst files")
        println("0x60 npc_crt invocations: $hits60")
        println("0xF94D invocations: $hitsF94D")
        println("Examples (window of 4 prev + the call):")
        examples.forEach { println("  $it") }
    }
}
