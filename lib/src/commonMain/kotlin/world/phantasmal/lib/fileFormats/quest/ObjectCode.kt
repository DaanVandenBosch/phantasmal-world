package world.phantasmal.lib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.PwResultBuilder
import world.phantasmal.core.Severity
import world.phantasmal.lib.assembly.*
import world.phantasmal.lib.assembly.dataFlowAnalysis.ControlFlowGraph
import world.phantasmal.lib.assembly.dataFlowAnalysis.getRegisterValue
import world.phantasmal.lib.assembly.dataFlowAnalysis.getStackValue
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.BufferCursor
import world.phantasmal.lib.cursor.Cursor
import kotlin.math.ceil
import kotlin.math.min

private val logger = KotlinLogging.logger {}

val SEGMENT_PRIORITY = mapOf(
    SegmentType.Instructions to 2,
    SegmentType.String to 1,
    SegmentType.Data to 0,
)

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
)

fun parseObjectCode(
    objectCode: Buffer,
    labelOffsets: IntArray,
    entryLabels: Set<Int>,
    lenient: Boolean,
    dcGcFormat: Boolean,
): PwResult<List<Segment>> {
    val cursor = BufferCursor(objectCode)
    val labelHolder = LabelHolder(labelOffsets)
    val result = PwResultBuilder<List<Segment>>(logger)
    val offsetToSegment = mutableMapOf<Int, Segment>()

    findAndParseSegments(
        cursor,
        labelHolder,
        entryLabels.map { it to SegmentType.Instructions }.toMap(),
        offsetToSegment,
        lenient,
        dcGcFormat,
    )

    val segments: MutableList<Segment> = mutableListOf()

    // Put segments in an array and parse left-over segments as data.
    var offset = 0

    while (offset < cursor.size.toInt()) {
        var segment: Segment? = offsetToSegment[offset]

        // If we have a segment, add it. Otherwise create a new data segment.
        if (segment == null) {
            val labels = labelHolder.getLabels(offset)
            var endOffset: Int

            if (labels == null) {
                endOffset = cursor.size.toInt()

                for (label in labelHolder.labels) {
                    if (label.offset > offset) {
                        endOffset = label.offset
                        break
                    }
                }
            } else {
                val info = labelHolder.getInfo(labels[0])!!
                endOffset = info.next?.offset ?: cursor.size.toInt()
            }

            cursor.seekStart(offset.toUInt())
            parseDataSegment(
                offsetToSegment,
                cursor,
                endOffset,
                labels?.toMutableList() ?: mutableListOf()
            )

            segment = offsetToSegment[offset]

            check(endOffset > offset) {
                "Next offset $endOffset was smaller than or equal to current offset ${offset}."
            }
            checkNotNull(segment) { "Couldn't create segment for offset ${offset}." }
        }

        segments.add(segment)

        offset += when (segment) {
            is InstructionSegment -> segment.instructions.sumBy { instructionSize(it, dcGcFormat) }

            is DataSegment -> segment.data.size.toInt()

            // String segments should be multiples of 4 bytes.
            is StringSegment -> 4 * ceil((segment.value.length + 1) / 2.0).toInt()
        }
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

    // Sanity check parsed object code.
    if (cursor.size != offset.toUInt()) {
        result.addProblem(
            Severity.Error,
            "The script code is corrupt.",
            "Expected to parse ${cursor.size} bytes but parsed $offset instead.",
        )

        if (!lenient) {
            return result.failure()
        }
    }

    return result.success(segments)
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

    // Iteratively parse segments from label references.
    do {
        startSegmentCount = offsetToSegment.size

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
            for (instruction in segment.instructions) {
                var i = 0

                while (i < instruction.opcode.params.size) {
                    val param = instruction.opcode.params[i]

                    when (param.type) {
                        is ILabelType ->
                            getArgLabelValues(
                                cfg,
                                newLabels,
                                instruction,
                                i,
                                SegmentType.Instructions,
                            )

                        is ILabelVarType -> {
                            // Never on the stack.
                            // Eat all remaining arguments.
                            while (i < instruction.args.size) {
                                newLabels[instruction.args[i].value as Int] = SegmentType.Instructions
                                i++
                            }
                        }

                        is DLabelType ->
                            getArgLabelValues(cfg, newLabels, instruction, i, SegmentType.Data)

                        is SLabelType ->
                            getArgLabelValues(cfg, newLabels, instruction, i, SegmentType.String)

                        is RegTupRefType -> {
                            // Never on the stack.
                            val arg = instruction.args[i]

                            for (j in param.type.registerTuples.indices) {
                                val regTup = param.type.registerTuples[j]

                                if (regTup.type is ILabelType) {
                                    val labelValues = getRegisterValue(
                                        cfg,
                                        instruction,
                                        arg.value as Int + j,
                                    )

                                    if (labelValues.size <= 10) {
                                        for (label in labelValues) {
                                            newLabels[label] = SegmentType.Instructions
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
    instruction: Instruction,
    paramIdx: Int,
    segmentType: SegmentType,
) {
    if (instruction.opcode.stack === StackInteraction.Pop) {
        val stackValues = getStackValue(
            cfg,
            instruction,
            instruction.opcode.params.size - paramIdx - 1,
        )

        if (stackValues.size <= 10) {
            for (value in stackValues) {
                val oldType = labels[value]

                if (
                    oldType == null ||
                    SEGMENT_PRIORITY.getValue(segmentType) > SEGMENT_PRIORITY.getValue(oldType)
                ) {
                    labels[value] = segmentType
                }
            }
        }
    } else {
        val value = instruction.args[paramIdx].value as Int
        val oldType = labels[value]

        if (
            oldType == null ||
            SEGMENT_PRIORITY.getValue(segmentType) > SEGMENT_PRIORITY.getValue(oldType)
        ) {
            labels[value] = segmentType
        }
    }
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

        val endOffset = info.next?.offset ?: cursor.size.toInt()
        cursor.seekStart(info.offset.toUInt())

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
    } catch (e: Throwable) {
        if (lenient) {
            logger.error(e) { "Couldn't fully parse object code segment." }
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
    offsetToSegment[cursor.position.toInt()] = segment

    while (cursor.position < endOffset.toUInt()) {
        // Parse the opcode.
        val mainOpcode = cursor.u8()

        val fullOpcode = when (mainOpcode.toInt()) {
            0xF8, 0xF9 -> ((mainOpcode.toUInt() shl 8) or cursor.u8().toUInt()).toInt()
            else -> mainOpcode.toInt()
        }

        val opcode = codeToOpcode(fullOpcode)

        // Parse the arguments.
        try {
            val args = parseInstructionArguments(cursor, opcode, dcGcFormat)
            instructions.add(Instruction(opcode, args, null))
        } catch (e: Throwable) {
            if (lenient) {
                logger.error(e) {
                    "Exception occurred while parsing arguments for instruction ${opcode.mnemonic}."
                }
                instructions.add(Instruction(opcode, emptyList(), null))
            } else {
                throw e
            }
        }
    }

    // Recurse on label drop-through.
    if (nextLabel != null) {
        // Find the first ret or jmp.
        var dropThrough = true

        for (i in instructions.size - 1 downTo 0) {
            val opcode = instructions[i].opcode

            if (opcode == OP_RET || opcode == OP_JMP) {
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
        cursor.buffer(endOffset.toUInt() - startOffset),
        SegmentSrcLoc(),
    )
    offsetToSegment[startOffset.toInt()] = segment
}

private fun parseStringSegment(
    offsetToSegment: MutableMap<Int, Segment>,
    cursor: Cursor,
    endOffset: Int,
    labels: MutableList<Int>,
    dcGcFormat: Boolean,
) {
    val startOffset = cursor.position
    val segment = StringSegment(
        labels,
        if (dcGcFormat) {
            cursor.stringAscii(
                endOffset.toUInt() - startOffset,
                nullTerminated = true,
                dropRemaining = true
            )
        } else {
            cursor.stringUtf16(
                endOffset.toUInt() - startOffset,
                nullTerminated = true,
                dropRemaining = true
            )
        },
        SegmentSrcLoc()
    )
    offsetToSegment[startOffset.toInt()] = segment
}

private fun parseInstructionArguments(
    cursor: Cursor,
    opcode: Opcode,
    dcGcFormat: Boolean,
): List<Arg> {
    val args = mutableListOf<Arg>()

    if (opcode.stack != StackInteraction.Pop) {
        for (param in opcode.params) {
            when (param.type) {
                is ByteType ->
                    args.add(Arg(cursor.u8().toInt()))

                is WordType ->
                    args.add(Arg(cursor.u16().toInt()))

                is DWordType ->
                    args.add(Arg(cursor.i32()))

                is FloatType ->
                    args.add(Arg(cursor.f32()))

                is LabelType,
                is ILabelType,
                is DLabelType,
                is SLabelType,
                -> {
                    args.add(Arg(cursor.u16().toInt()))
                }

                is StringType -> {
                    val maxBytes = min(4096u, cursor.bytesLeft)
                    args.add(Arg(
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
                    ))
                }

                is ILabelVarType -> {
                    val argSize = cursor.u8()
                    args.addAll(cursor.u16Array(argSize.toUInt()).map { Arg(it.toInt()) })
                }

                is RegRefType,
                is RegTupRefType,
                -> {
                    args.add(Arg(cursor.u8().toInt()))
                }

                is RegRefVarType -> {
                    val argSize = cursor.u8()
                    args.addAll(cursor.u8Array(argSize.toUInt()).map { Arg(it.toInt()) })
                }

                else -> error("Parameter type ${param.type} not implemented.")
            }
        }
    }

    return args
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
