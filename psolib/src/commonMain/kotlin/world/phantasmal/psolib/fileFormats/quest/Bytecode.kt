package world.phantasmal.psolib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.asm.dataFlowAnalysis.ControlFlowGraph
import world.phantasmal.psolib.asm.dataFlowAnalysis.getRegisterValue
import world.phantasmal.psolib.asm.dataFlowAnalysis.getStackValue
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.BufferCursor
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor
import kotlin.math.min

private val logger = KotlinLogging.logger {}

private const val MAX_TOTAL_NOPS = 20
private const val MAX_SEQUENTIAL_NOPS = 10
private const val MAX_UNKNOWN_OPCODE_RATIO = 0.2
private const val MAX_STACK_POP_WITHOUT_PRECEDING_PUSH_RATIO = 0.2
private const val MAX_UNKNOWN_LABEL_RATIO = 0.2

val SEGMENT_PRIORITY = mapOf(
    SegmentType.Instructions to 2,
    SegmentType.String to 1,
    SegmentType.Data to 0,
)

/**
 * These functions are built into the client and can optionally be overridden on BB. Other versions
 * require you to always specify them in the script.
 */
val BUILTIN_FUNCTIONS = setOf(
    60,
    70,
    80,
    90,
    100,
    110,
    120,
    130,
    140,
    800,
    810,
    820,
    830,
    840,
    850,
    860,
    900,
    910,
    920,
    930,
    940,
    950,
    960,
)

/**
 * Parses bytecode into bytecode IR.
 */
fun parseBytecode(
    bytecode: Buffer,
    labelOffsets: IntArray,
    entryLabels: Set<Int>,
    dcGcFormat: Boolean,
    lenient: Boolean,
): PwResult<BytecodeIr> {
    val cursor = BufferCursor(bytecode)
    val labelHolder = LabelHolder(labelOffsets)
    val result = PwResult.build<BytecodeIr>(logger)
    val offsetToSegment = mutableMapOf<Int, Segment>()

    findAndParseSegments(
        cursor,
        labelHolder,
        entryLabels.associateWith { SegmentType.Instructions },
        offsetToSegment,
        lenient,
        dcGcFormat,
    )

    val segments: MutableList<Segment> = mutableListOf()

    // Put segments in an array and try to parse leftover segments as instructions segments. When a
    // segment can't be parsed as instructions, fall back to parsing it as a data segment.
    var offset = 0

    while (offset < cursor.size) {
        var segment: Segment? = offsetToSegment[offset]

        // If we have a segment, add it. Otherwise create a new data segment.
        if (segment == null) {
            val labels = labelHolder.getLabels(offset)
            var endOffset: Int

            if (labels == null) {
                endOffset = cursor.size

                for (label in labelHolder.labels) {
                    if (label.offset > offset) {
                        endOffset = label.offset
                        break
                    }
                }
            } else {
                val info = labelHolder.getInfo(labels[0])!!
                endOffset = info.next?.offset ?: cursor.size
            }

            cursor.seekStart(offset)

            val isInstructionsSegment = tryParseInstructionsSegment(
                offsetToSegment,
                labelHolder,
                cursor,
                endOffset,
                labels?.toMutableList() ?: mutableListOf(),
                dcGcFormat,
            )

            if (!isInstructionsSegment) {
                cursor.seekStart(offset)

                parseDataSegment(
                    offsetToSegment,
                    cursor,
                    endOffset,
                    labels?.toMutableList() ?: mutableListOf()
                )
            }

            segment = offsetToSegment[offset]

            check(endOffset > offset) {
                "Next offset $endOffset was smaller than or equal to current offset ${offset}."
            }
            checkNotNull(segment) { "Couldn't create segment for offset ${offset}." }
        }

        segments.add(segment)

        offset += segment.size(dcGcFormat)
    }

    // Add unreferenced labels to their segment.
    for ((label, labelOffset) in labelHolder.labels) {
        val segment = offsetToSegment[labelOffset]

        if (segment == null) {
            result.addProblem(
                Severity.Warning,
                "Label $label doesn't point to anything.",
                "Label $label with offset $labelOffset doesn't point to anything.",
            )
        } else {
            if (label !in segment.labels) {
                segment.labels.add(label)
                segment.labels.sort()
            }
        }
    }

    // Sanity check parsed byte code.
    if (cursor.size != offset) {
        result.addProblem(
            Severity.Error,
            "The script code is corrupt.",
            "Expected to parse ${cursor.size} bytes but parsed $offset instead.",
        )

        if (!lenient) {
            return result.failure()
        }
    }

    return result.success(BytecodeIr(segments))
}

private fun findAndParseSegments(
    cursor: Cursor,
    labelHolder: LabelHolder,
    labels: Map<Int, SegmentType>,
    offsetToSegment: MutableMap<Int, Segment>,
    lenient: Boolean,
    dcGcFormat: Boolean,
) {
    var newLabels = labels
    var startSegmentCount: Int
    // Instruction segments which we've been able to fully analyze for label references so far.
    val analyzedSegments = mutableSetOf<InstructionSegment>()

    // Iteratively parse segments from label references.
    do {
        startSegmentCount = offsetToSegment.size

        // Parse segments of which the type is known.
        for ((label, type) in newLabels) {
            parseSegment(offsetToSegment, labelHolder, cursor, label, type, lenient, dcGcFormat)
        }

        // Find label references.
        val sortedSegments = offsetToSegment.entries
            .filter { (_, s) -> s is InstructionSegment }
            .sortedBy { it.key }
            .map { (_, s) -> s as InstructionSegment }

        val cfg = ControlFlowGraph.create(sortedSegments)

        newLabels = mutableMapOf()

        for (segment in sortedSegments) {
            if (segment in analyzedSegments) continue

            var foundAllLabels = true

            for (instructionIdx in segment.instructions.indices) {
                val instruction = segment.instructions[instructionIdx]
                var i = 0

                while (i < instruction.opcode.params.size) {
                    val param = instruction.opcode.params[i]

                    when (param.type) {
                        is ILabelType -> {
                            if (!getArgLabelValues(
                                    cfg,
                                    newLabels,
                                    segment,
                                    instructionIdx,
                                    i,
                                    SegmentType.Instructions,
                                )
                            ) {
                                foundAllLabels = false
                            }
                        }

                        is ILabelVarType -> {
                            // Never on the stack.
                            // Eat all remaining arguments.
                            while (i < instruction.args.size) {
                                newLabels[(instruction.args[i] as IntArg).value] =
                                    SegmentType.Instructions

                                i++
                            }
                        }

                        is DLabelType -> {
                            if (!getArgLabelValues(
                                    cfg,
                                    newLabels,
                                    segment,
                                    instructionIdx,
                                    i,
                                    SegmentType.Data
                                )
                            ) {
                                foundAllLabels = false
                            }
                        }

                        is SLabelType -> {
                            if (!getArgLabelValues(
                                    cfg,
                                    newLabels,
                                    segment,
                                    instructionIdx,
                                    i,
                                    SegmentType.String
                                )
                            ) {
                                foundAllLabels = false
                            }
                        }

                        is RegType -> if (param.type.registers != null) {
                            for (j in param.type.registers.indices) {
                                val registerParam = param.type.registers[j]

                                // Never on the stack.
                                if (registerParam.type is ILabelType) {
                                    val firstRegister = instruction.args[0].value as Int
                                    val labelValues = getRegisterValue(
                                        cfg,
                                        instruction,
                                        firstRegister + j,
                                    )

                                    if (labelValues.size <= 20) {
                                        for (label in labelValues) {
                                            newLabels[label] = SegmentType.Instructions
                                        }
                                    } else {
                                        foundAllLabels = false
                                    }
                                }
                            }
                        }

                        else -> {}
                    }

                    i++
                }
            }

            if (foundAllLabels) {
                analyzedSegments.add(segment)
            }
        }
    } while (offsetToSegment.size > startSegmentCount)
}

/**
 * Returns immediate arguments or stack arguments.
 */
private fun getArgLabelValues(
    cfg: ControlFlowGraph,
    labels: MutableMap<Int, SegmentType>,
    instructionSegment: InstructionSegment,
    instructionIdx: Int,
    paramIdx: Int,
    segmentType: SegmentType,
): Boolean {
    val instruction = instructionSegment.instructions[instructionIdx]

    if (instruction.opcode.stack === StackInteraction.Pop) {
        val stackValues = getStackValue(
            cfg,
            instruction,
            instruction.opcode.params.size - paramIdx - 1,
        ).first

        if (stackValues.size <= 20) {
            for (value in stackValues) {
                val oldType = labels[value]

                if (
                    oldType == null ||
                    SEGMENT_PRIORITY.getValue(segmentType) > SEGMENT_PRIORITY.getValue(oldType)
                ) {
                    labels[value] = segmentType
                }
            }

            return true
        }
    } else {
        val value = (instruction.args[paramIdx] as IntArg).value
        val oldType = labels[value]

        if (
            oldType == null ||
            SEGMENT_PRIORITY.getValue(segmentType) > SEGMENT_PRIORITY.getValue(oldType)
        ) {
            labels[value] = segmentType
        }

        return true
    }

    return false
}

private fun parseSegment(
    offsetToSegment: MutableMap<Int, Segment>,
    labelHolder: LabelHolder,
    cursor: Cursor,
    label: Int,
    type: SegmentType,
    lenient: Boolean,
    dcGcFormat: Boolean,
) {
    try {
        val info = labelHolder.getInfo(label)

        if (info == null) {
            if (label !in BUILTIN_FUNCTIONS) {
                logger.warn { "Label $label is not registered in the label table." }
            }

            return
        }

        // Check whether we've already parsed this segment and reparse it if necessary.
        val segment = offsetToSegment[info.offset]

        val labels: MutableList<Int> =
            if (segment == null) {
                mutableListOf(label)
            } else {
                if (label !in segment.labels) {
                    segment.labels.add(label)
                    segment.labels.sort()
                }

                if (SEGMENT_PRIORITY.getValue(type) > SEGMENT_PRIORITY.getValue(segment.type)) {
                    segment.labels
                } else {
                    return
                }
            }

        val endOffset = info.next?.offset ?: cursor.size
        cursor.seekStart(info.offset)

        return when (type) {
            SegmentType.Instructions ->
                parseInstructionsSegment(
                    offsetToSegment,
                    labelHolder,
                    cursor,
                    endOffset,
                    labels,
                    info.next?.label,
                    lenient,
                    dcGcFormat,
                )

            SegmentType.Data ->
                parseDataSegment(offsetToSegment, cursor, endOffset, labels)

            SegmentType.String ->
                parseStringSegment(offsetToSegment, cursor, endOffset, labels, dcGcFormat)
        }
    } catch (e: Exception) {
        if (lenient) {
            logger.error(e) { "Couldn't fully parse byte code segment." }
        } else {
            throw e
        }
    }
}

private fun parseInstructionsSegment(
    offsetToSegment: MutableMap<Int, Segment>,
    labelHolder: LabelHolder,
    cursor: Cursor,
    endOffset: Int,
    labels: MutableList<Int>,
    nextLabel: Int?,
    lenient: Boolean,
    dcGcFormat: Boolean,
) {
    val instructions = mutableListOf<Instruction>()

    val segment = InstructionSegment(
        labels,
        instructions,
        SegmentSrcLoc()
    )
    offsetToSegment[cursor.position] = segment

    while (cursor.position < endOffset) {
        // Parse the opcode.
        val mainOpcode = cursor.uByte()

        val fullOpcode = when (mainOpcode.toInt()) {
            0xF8, 0xF9 -> ((mainOpcode.toInt() shl 8) or cursor.uByte().toInt())
            else -> mainOpcode.toInt()
        }

        val opcode = codeToOpcode(fullOpcode)

        // Parse the arguments.
        try {
            val args = parseInstructionArguments(cursor, opcode, dcGcFormat)
            instructions.add(Instruction(opcode, args, srcLoc = null, valid = true))
        } catch (e: Exception) {
            if (lenient) {
                logger.error(e) {
                    "Exception occurred while parsing arguments for instruction ${opcode.mnemonic}."
                }
                instructions.add(Instruction(opcode, emptyList(), srcLoc = null, valid = false))
            } else {
                throw e
            }
        }
    }

    // Recurse on label drop-through.
    if (nextLabel != null) {
        // Find the last ret or jmp.
        var dropThrough = true

        for (i in instructions.lastIndex downTo 0) {
            val opcode = instructions[i].opcode.code

            if (opcode == OP_RET.code || opcode == OP_JMP.code) {
                dropThrough = false
                break
            }
        }

        if (dropThrough) {
            parseSegment(
                offsetToSegment,
                labelHolder,
                cursor,
                nextLabel,
                SegmentType.Instructions,
                lenient,
                dcGcFormat,
            )
        }
    }
}

private fun parseDataSegment(
    offsetToSegment: MutableMap<Int, Segment>,
    cursor: Cursor,
    endOffset: Int,
    labels: MutableList<Int>,
) {
    val startOffset = cursor.position
    val segment = DataSegment(
        labels,
        cursor.buffer(endOffset - startOffset),
        SegmentSrcLoc(),
    )
    offsetToSegment[startOffset] = segment
}

private fun parseStringSegment(
    offsetToSegment: MutableMap<Int, Segment>,
    cursor: Cursor,
    endOffset: Int,
    labels: MutableList<Int>,
    dcGcFormat: Boolean,
) {
    val startOffset = cursor.position
    val byteLength = endOffset - startOffset
    val segment = StringSegment(
        labels,
        if (dcGcFormat) {
            cursor.stringAscii(
                byteLength,
                nullTerminated = true,
                dropRemaining = true
            )
        } else {
            cursor.stringUtf16(
                byteLength,
                nullTerminated = true,
                dropRemaining = true
            )
        },
        byteLength,
        SegmentSrcLoc()
    )
    offsetToSegment[startOffset] = segment
}

private fun parseInstructionArguments(
    cursor: Cursor,
    opcode: Opcode,
    dcGcFormat: Boolean,
): List<Arg> {
    val args = mutableListOf<Arg>()

    if (opcode.stack != StackInteraction.Pop) {
        var varargCount = 0

        for (param in opcode.params) {
            when (param.type) {
                is ByteType ->
                    args.add(IntArg(cursor.uByte().toInt()))

                is ShortType ->
                    args.add(IntArg(cursor.uShort().toInt()))

                is IntType ->
                    args.add(IntArg(cursor.int()))

                is FloatType ->
                    args.add(FloatArg(cursor.float()))

                // Ensure this case is before the LabelType case because ILabelVarType extends
                // LabelType.
                is ILabelVarType -> {
                    varargCount++
                    val argSize = cursor.uByte()
                    args.addAll(cursor.uShortArray(argSize.toInt()).map { IntArg(it.toInt()) })
                }

                is LabelType -> {
                    args.add(IntArg(cursor.uShort().toInt()))
                }

                is StringType -> {
                    val maxBytes = min(4096, cursor.bytesLeft)
                    args.add(
                        StringArg(
                            if (dcGcFormat) {
                                cursor.stringAscii(
                                    maxBytes,
                                    nullTerminated = true,
                                    dropRemaining = false
                                )
                            } else {
                                cursor.stringUtf16(
                                    maxBytes,
                                    nullTerminated = true,
                                    dropRemaining = false
                                )
                            },
                        )
                    )
                }

                is RegType -> {
                    args.add(IntArg(cursor.uByte().toInt()))
                }

                is RegVarType -> {
                    varargCount++
                    val argSize = cursor.uByte()
                    args.addAll(cursor.uByteArray(argSize.toInt()).map { IntArg(it.toInt()) })
                }

                else -> error("Parameter type ${param.type} not implemented.")
            }
        }

        val minExpectedArgs = opcode.params.size - varargCount

        check(args.size >= minExpectedArgs) {
            "Expected to parse at least $minExpectedArgs, only parsed ${args.size}."
        }
    }

    return args
}

private fun tryParseInstructionsSegment(
    offsetToSegment: MutableMap<Int, Segment>,
    labelHolder: LabelHolder,
    cursor: Cursor,
    endOffset: Int,
    labels: MutableList<Int>,
    dcGcFormat: Boolean,
): Boolean {
    val offset = cursor.position

    fun logReason(reason: String, t: Throwable? = null) {
        logger.trace(t) {
            buildString {
                append("Determined that segment ")

                if (labels.isEmpty()) {
                    append("without label")
                } else {
                    if (labels.size == 1) append("with label ")
                    else append("with labels ")

                    labels.joinTo(this)
                }

                append(" at offset ")
                append(offset)
                append(" is not an instructions segment because ")
                append(reason)
                append(".")
            }
        }
    }

    try {
        parseInstructionsSegment(
            offsetToSegment,
            labelHolder,
            cursor,
            endOffset,
            labels,
            nextLabel = null,
            lenient = false,
            dcGcFormat,
        )

        val segment = offsetToSegment[offset]
        val instructions = (segment as InstructionSegment).instructions

        // Heuristically try to detect whether the segment is actually a data segment.
        var prevOpcode: Opcode? = null
        var totalNopCount = 0
        var sequentialNopCount = 0
        var unknownOpcodeCount = 0
        var stackPopCount = 0
        var stackPopWithoutPrecedingPushCount = 0
        var labelCount = 0
        var unknownLabelCount = 0

        for (inst in instructions) {
            if (inst.opcode.code == OP_NOP.code) {
                if (++totalNopCount > MAX_TOTAL_NOPS) {
                    logReason("it has more than $MAX_TOTAL_NOPS nop instructions")
                    return false
                }

                if (++sequentialNopCount > MAX_SEQUENTIAL_NOPS) {
                    logReason("it has more than $MAX_SEQUENTIAL_NOPS sequential nop instructions")
                    return false
                }
            } else {
                sequentialNopCount = 0
            }

            if (!inst.opcode.known) {
                unknownOpcodeCount++
            }

            if (inst.opcode.stack == StackInteraction.Pop) {
                stackPopCount++

                if (prevOpcode?.stack != StackInteraction.Push) {
                    stackPopWithoutPrecedingPushCount++
                }
            }

            for ((index, param) in inst.opcode.params.withIndex()) {
                if (index >= inst.args.size) break

                if (param.type is LabelType) {
                    for (arg in inst.getArgs(index)) {
                        labelCount++

                        if (!labelHolder.hasLabel((arg as IntArg).value)) {
                            unknownLabelCount++
                        }
                    }
                }
            }

            prevOpcode = inst.opcode
        }

        val unknownLabelRatio = unknownLabelCount.toDouble() / labelCount

        if (unknownLabelRatio > MAX_UNKNOWN_LABEL_RATIO) {
            logReason(
                "${100 * unknownLabelRatio}% of its label references are to nonexistent labels"
            )
            return false
        }

        val stackPopWithoutPrecedingPushRatio =
            stackPopWithoutPrecedingPushCount.toDouble() / stackPopCount

        if (stackPopWithoutPrecedingPushRatio > MAX_STACK_POP_WITHOUT_PRECEDING_PUSH_RATIO) {
            logReason(
                "${100 * stackPopWithoutPrecedingPushRatio}% of its stack pop instructions don't have a preceding push instruction"
            )
            return false
        }

        val unknownOpcodeRatio = unknownOpcodeCount.toDouble() / instructions.size

        if (unknownOpcodeRatio > MAX_UNKNOWN_OPCODE_RATIO) {
            logReason("${100 * unknownOpcodeRatio}% of its opcodes are unknown")
            return false
        }

        return true
    } catch (e: Exception) {
        logReason("parsing it resulted in an exception", e)
        return false
    }
}

fun writeBytecode(bytecodeIr: BytecodeIr, dcGcFormat: Boolean): BytecodeAndLabelOffsets {
    val buffer = Buffer.withCapacity(100 * bytecodeIr.segments.size, Endianness.Little)
    val cursor = buffer.cursor()
    // Keep track of label offsets.
    val largestLabel = bytecodeIr.segments.asSequence().flatMap { it.labels }.maxOrNull() ?: -1
    val labelOffsets = IntArray(largestLabel + 1) { -1 }

    for (segment in bytecodeIr.segments) {
        for (label in segment.labels) {
            labelOffsets[label] = cursor.position
        }

        when (segment) {
            is InstructionSegment -> {
                for (instruction in segment.instructions) {
                    val opcode = instruction.opcode

                    if (opcode.size == 2) {
                        cursor.writeByte((opcode.code ushr 8).toByte())
                    }

                    cursor.writeByte(opcode.code.toByte())

                    if (opcode.stack != StackInteraction.Pop) {
                        for (i in opcode.params.indices) {
                            val param = opcode.params[i]
                            val args = instruction.getArgs(i)
                            val arg = args.firstOrNull()

                            if (arg == null) {
                                logger.warn {
                                    "No argument passed to ${opcode.mnemonic} for parameter ${i + 1}."
                                }
                                continue
                            }

                            when (param.type) {
                                ByteType -> cursor.writeByte(arg.coerceInt().toByte())
                                ShortType -> cursor.writeShort(arg.coerceInt().toShort())
                                IntType -> cursor.writeInt(arg.coerceInt())
                                FloatType -> cursor.writeFloat(arg.coerceFloat())
                                // Ensure this case is before the LabelType case because
                                // ILabelVarType extends LabelType.
                                ILabelVarType -> {
                                    cursor.writeByte(args.size.toByte())

                                    for (a in args) {
                                        cursor.writeShort(a.coerceInt().toShort())
                                    }
                                }

                                is LabelType -> cursor.writeShort(arg.coerceInt().toShort())

                                StringType -> {
                                    val str = arg.coerceString()

                                    if (dcGcFormat) cursor.writeStringAscii(str, str.length + 1)
                                    else cursor.writeStringUtf16(str, 2 * str.length + 2)
                                }

                                is RegType -> {
                                    cursor.writeByte(arg.coerceInt().toByte())
                                }

                                RegVarType -> {
                                    cursor.writeByte(args.size.toByte())

                                    for (a in args) {
                                        cursor.writeByte(a.coerceInt().toByte())
                                    }
                                }

                                else -> error(
                                    "Parameter type ${param.type::class.simpleName} not supported."
                                )
                            }
                        }
                    }
                }
            }

            is StringSegment -> {
                // String segments should be multiples of 4 bytes.
                if (dcGcFormat) {
                    cursor.writeStringAscii(segment.value, segment.size(dcGcFormat))
                } else {
                    cursor.writeStringUtf16(segment.value, segment.size(dcGcFormat))
                }
            }

            is DataSegment -> {
                cursor.writeCursor(segment.data.cursor())
            }
        }
    }

    return BytecodeAndLabelOffsets(buffer, labelOffsets)
}

class BytecodeAndLabelOffsets(
    val bytecode: Buffer,
    val labelOffsets: IntArray,
) {
    operator fun component1(): Buffer = bytecode
    operator fun component2(): IntArray = labelOffsets
}

private data class LabelAndOffset(val label: Int, val offset: Int)
private data class OffsetAndIndex(val offset: Int, val index: Int)
private class LabelInfo(val offset: Int, val next: LabelAndOffset?)

private class LabelHolder(labelOffsets: IntArray) {
    /**
     * Mapping of labels to their offset and index into [labels].
     */
    private val labelMap: MutableMap<Int, OffsetAndIndex> = mutableMapOf()

    /**
     * Mapping of offsets to lists of labels.
     */
    private val offsetMap: MutableMap<Int, MutableList<Int>> = mutableMapOf()

    /**
     * Labels and their offset sorted by offset and then label.
     */
    val labels: List<LabelAndOffset>

    init {
        val labels = mutableListOf<LabelAndOffset>()

        // Populate the main label list.
        for (label in labelOffsets.indices) {
            val offset = labelOffsets[label]

            if (offset != -1) {
                labels.add(LabelAndOffset(label, offset))
            }
        }

        // Sort by offset, then label.
        labels.sortWith { a, b ->
            if (a.offset - b.offset != 0) a.offset - b.offset
            else a.label - b.label
        }

        this.labels = labels

        // Populate the label and offset maps.
        for (index in 0 until labels.size) {
            val (label, offset) = labels[index]

            labelMap[label] = OffsetAndIndex(offset, index)

            offsetMap.getOrPut(offset) { mutableListOf() }.add(label)
        }
    }

    fun hasLabel(label: Int): Boolean = label in labelMap

    fun getLabels(offset: Int): List<Int>? = offsetMap[offset]

    fun getInfo(label: Int): LabelInfo? {
        val offsetAndIndex = labelMap[label] ?: return null

        // Find the next label with a different offset.
        var next: LabelAndOffset? = null

        for (i in offsetAndIndex.index + 1 until labels.size) {
            next = labels[i]

            // Skip the label if it points to the same offset.
            if (next.offset > offsetAndIndex.offset) {
                break
            } else {
                next = null
            }
        }

        return LabelInfo(offsetAndIndex.offset, next)
    }
}
