package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.core.Success
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.readFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Generates a test .qst file based on seat_of_the_heart (Seat of the Heart, EP1) with an additional
 * data segment containing BattleParamEntry enemy data referenced by get_physical_data.
 *
 * The generated file can be loaded in the quest editor to verify that enemy data blocks are
 * correctly displayed as HEX data instead of being misinterpreted as opcodes.
 */
class GenerateTestQst : LibTestSuite {

    @Test
    fun generate_quest_with_enemy_data() = testAsync {
        val result = parseQstToQuest(readFile("/seat_of_the_heart.qst"))
        assertTrue(result is Success)

        val quest = result.value.quest

        // Find the highest label used in existing bytecodeIr
        val maxLabel = quest.bytecodeIr.segments.flatMap { it.labels }.maxOrNull() ?: 0
        val dataLabel = maxLabel + 1

        // BattleParamEntry stats data (36 bytes):
        // ATP=140, MST=0, EVP=22, HP=180, DFP=30, ATA=80, LCK=10, ESP=10,
        // float=30.0, float=19.0, unk, unk, EXP=40, meseta=0
        val enemyData = Buffer.fromByteArray(
            ubyteArrayOf(
                0x8Cu, 0x00u, 0x00u, 0x00u, 0x16u, 0x00u, 0xB4u, 0x00u,
                0x1Eu, 0x00u, 0x50u, 0x00u, 0x0Au, 0x00u, 0x0Au, 0x00u,
                0x00u, 0x00u, 0xF0u, 0x41u, 0x00u, 0x00u, 0x98u, 0x41u,
                0x00u, 0x00u, 0x37u, 0x00u, 0x28u, 0x00u, 0x00u, 0x00u,
                0x00u, 0x00u, 0x00u, 0x00u,
            ).toByteArray()
        )

        // Add get_physical_data instruction to the first instruction segment (label 0),
        // right before the existing ret instruction
        val label0Segment = quest.bytecodeIr.segments
            .filterIsInstance<InstructionSegment>()
            .first { 0 in it.labels }

        val retIndex = label0Segment.instructions.indexOfLast { it.opcode == OP_RET }
        if (retIndex >= 0) {
            label0Segment.instructions.add(
                retIndex,
                Instruction(
                    OP_GET_PHYSICAL_DATA,
                    listOf(IntArg(dataLabel)),
                    valid = true,
                    srcLoc = null,
                ),
            )
        }

        // Add data segment with enemy data
        val newSegments = quest.bytecodeIr.segments.toMutableList()
        newSegments.add(DataSegment(mutableListOf(dataLabel), enemyData))
        quest.bytecodeIr = BytecodeIr(newSegments)

        // Write to .qst file
        val qstBuffer = writeQuestToQst(
            quest,
            "seat_of_the_heart_enemy_data",
            result.value.version,
            result.value.online,
        )

        val projectRoot = File(System.getProperty("user.dir")).let {
            if (it.name == "psolib") it.parentFile else it
        }
        val outputFile = File(projectRoot, "tmp/seat_of_the_heart_enemy_data.qst")
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(qstBuffer.byteArray.copyOf(qstBuffer.size))

        println("Generated: ${outputFile.absolutePath}")
        println("Data label: $dataLabel")
        println("Quest: ${quest.name}")
        assertTrue(outputFile.exists())
    }
}
