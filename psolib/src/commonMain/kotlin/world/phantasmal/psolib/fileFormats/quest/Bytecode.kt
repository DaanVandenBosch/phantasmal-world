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
private const val MAX_LABEL_VALUES = 20
/**
 * Segments with this many or fewer instructions are considered "short" for the purpose of
 * unknown-opcode rejection. See [isLikelyInstructionSegment] for details.
 */
private const val MAX_SHORT_SEGMENT_SIZE = 10

internal val SEGMENT_PRIORITY = mapOf(
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
    stringEncoding: BytecodeStringEncoding,
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
        stringEncoding,
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
                stringEncoding,
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

        offset += segment.size(stringEncoding)
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

    val ir = BytecodeIr(segments)
    normalizeStackArgs(ir)
    return result.success(ir)
}

private fun findAndParseSegments(
    cursor: Cursor,
    labelHolder: LabelHolder,
    labels: Map<Int, SegmentType>,
    offsetToSegment: MutableMap<Int, Segment>,
    lenient: Boolean,
    stringEncoding: BytecodeStringEncoding,
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
            parseSegment(offsetToSegment, labelHolder, cursor, label, type, lenient, stringEncoding)
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
                if (!collectLabelReferencesFromInstruction(
                        cfg,
                        newLabels,
                        segment,
                        instructionIdx,
                    )
                ) {
                    foundAllLabels = false
                }
            }

            if (foundAllLabels) {
                analyzedSegments.add(segment)
            }
        }
    } while (offsetToSegment.size > startSegmentCount)
}

/**
 * Processes a single instruction's parameters to collect label references.
 * Returns true if all label references could be resolved.
 */
private fun collectLabelReferencesFromInstruction(
    cfg: ControlFlowGraph,
    newLabels: MutableMap<Int, SegmentType>,
    segment: InstructionSegment,
    instructionIdx: Int,
): Boolean {
    val instruction = segment.instructions[instructionIdx]
    var i = 0
    var foundAllLabels = true

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

                        if (labelValues.size <= MAX_LABEL_VALUES) {
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

    return foundAllLabels
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
        // Post-normalization: args are inlined directly on the Pop instruction.
        if (instruction.args.isNotEmpty()) {
            val arg = instruction.args.getOrNull(paramIdx) as? IntArg ?: return false
            // Register references (arg_pushr) cannot be statically resolved to a label value.
            if (arg.isRegRef) return false
            val value = arg.value
            val oldType = labels[value]

            if (
                oldType == null ||
                SEGMENT_PRIORITY.getValue(segmentType) > SEGMENT_PRIORITY.getValue(oldType)
            ) {
                labels[value] = segmentType
            }

            return true
        }

        // Pre-normalization: args are on the stack, use data flow analysis.
        val stackValues = getStackValue(
            cfg,
            instruction,
            instruction.opcode.params.size - paramIdx - 1,
        ).first

        if (stackValues.size <= MAX_LABEL_VALUES) {
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
    stringEncoding: BytecodeStringEncoding,
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
                    stringEncoding,
                )

            SegmentType.Data ->
                parseDataSegment(offsetToSegment, cursor, endOffset, labels)

            SegmentType.String ->
                parseStringSegment(offsetToSegment, cursor, endOffset, labels, stringEncoding)
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
    stringEncoding: BytecodeStringEncoding,
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
            val args = parseInstructionArguments(cursor, opcode, stringEncoding)
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
                stringEncoding,
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
    stringEncoding: BytecodeStringEncoding,
) {
    val startOffset = cursor.position
    val byteLength = endOffset - startOffset
    val segment = StringSegment(
        labels,
        when (stringEncoding) {
            BytecodeStringEncoding.ASCII -> cursor.stringAscii(
                byteLength,
                nullTerminated = true,
                dropRemaining = true,
            )

            BytecodeStringEncoding.UTF16 -> cursor.stringUtf16(
                byteLength,
                nullTerminated = true,
                dropRemaining = true,
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
    stringEncoding: BytecodeStringEncoding,
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
                            when (stringEncoding) {
                                BytecodeStringEncoding.ASCII -> cursor.stringAscii(
                                    maxBytes,
                                    nullTerminated = true,
                                    dropRemaining = false,
                                )

                                BytecodeStringEncoding.UTF16 -> cursor.stringUtf16(
                                    maxBytes,
                                    nullTerminated = true,
                                    dropRemaining = false,
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
    stringEncoding: BytecodeStringEncoding,
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
            stringEncoding,
        )

        val segment = offsetToSegment[offset]
        val instructions = (segment as InstructionSegment).instructions

        return isLikelyInstructionSegment(instructions, labelHolder) { reason ->
            logReason(reason)
        }
    } catch (e: Exception) {
        logReason("parsing it resulted in an exception", e)
        return false
    }
}

/**
 * Heuristically try to detect whether parsed instructions are likely a real instruction segment
 * (as opposed to data that happened to parse as valid opcodes).
 */
private fun isLikelyInstructionSegment(
    instructions: List<Instruction>,
    labelHolder: LabelHolder,
    logReason: (String) -> Unit,
): Boolean {
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

    if (labelCount > 0) {
        val unknownLabelRatio = unknownLabelCount.toDouble() / labelCount

        if (unknownLabelRatio > MAX_UNKNOWN_LABEL_RATIO) {
            logReason(
                "${100 * unknownLabelRatio}% of its label references are to nonexistent labels"
            )
            return false
        }
    }

    if (stackPopCount > 0) {
        val stackPopWithoutPrecedingPushRatio =
            stackPopWithoutPrecedingPushCount.toDouble() / stackPopCount

        if (stackPopWithoutPrecedingPushRatio > MAX_STACK_POP_WITHOUT_PRECEDING_PUSH_RATIO) {
            logReason(
                "${100 * stackPopWithoutPrecedingPushRatio}% of its stack pop instructions don't have a preceding push instruction"
            )
            return false
        }
    }

    if (instructions.isNotEmpty()) {
        val unknownOpcodeRatio = unknownOpcodeCount.toDouble() / instructions.size

        if (unknownOpcodeRatio > MAX_UNKNOWN_OPCODE_RATIO) {
            logReason("${100 * unknownOpcodeRatio}% of its opcodes are unknown")
            return false
        }

        // Stricter check for short segments: reject if ANY unknown opcode is present.
        //
        // Background: unreferenced labels (not discovered by findAndParseSegments) are
        // classified by this heuristic. Some labels point to non-code data (e.g. UTF-16
        // strings) whose bytes happen to decode as a mix of valid and unknown opcodes.
        //
        // The general unknownOpcodeRatio check above uses a 20% threshold, which can
        // produce inconsistent results for the same label across parse passes when push
        // normalization changes instruction encoding sizes (e.g. arg_pushl 5B → arg_pushw
        // 3B for LabelType params). This shifts neighboring segment boundaries, changing
        // the byte range evaluated for the unreferenced label, and the ratio may cross
        // the threshold in one direction only.
        //
        // For short segments (≤10 instructions), even one unknown opcode is a strong
        // signal that the bytes are data, not code. Rejecting unconditionally makes the
        // classification deterministic regardless of segment boundary variations.
        //
        // Observed in: clarie's deal.qst (EP4), label 64 — UTF-16 Japanese string data
        // misclassified as InstructionSegment on first parse, DataSegment after round-trip.
        if (unknownOpcodeCount > 0 && instructions.size <= MAX_SHORT_SEGMENT_SIZE) {
            logReason(
                "short segment (${instructions.size} instructions) contains " +
                    "$unknownOpcodeCount unknown opcode(s)"
            )
            return false
        }
    }

    return true
}

fun writeBytecode(
    bytecodeIr: BytecodeIr,
    stringEncoding: BytecodeStringEncoding,
): BytecodeAndLabelOffsets {
    val buffer = Buffer.withCapacity(100 * bytecodeIr.segments.size, Endianness.Little)
    val cursor = buffer.cursor()
    // Keep track of label offsets.
    val largestLabel = bytecodeIr.segments.asSequence().flatMap { it.labels }.maxOrNull() ?: -1
    val labelOffsets = IntArray(largestLabel + 1) { -1 }

    for (segment in bytecodeIr.segments) {
        val segmentStart = cursor.position

        for (label in segment.labels) {
            labelOffsets[label] = cursor.position
        }

        when (segment) {
            is InstructionSegment -> {
                for (instruction in segment.instructions) {
                    val opcode = instruction.opcode

                    if (opcode.stack == StackInteraction.Pop) {
                        // Write push instructions before the Pop opcode.
                        writePushInstructions(cursor, instruction, stringEncoding)
                    }

                    if (opcode.size == 2) {
                        cursor.writeByte((opcode.code ushr 8).toByte())
                    }

                    cursor.writeByte(opcode.code.toByte())

                    if (opcode.stack != StackInteraction.Pop) {
                        writeInlineArgs(cursor, instruction, stringEncoding)
                    }
                }
            }

            is StringSegment -> {
                val size = segment.size(stringEncoding)

                when (stringEncoding) {
                    BytecodeStringEncoding.ASCII ->
                        cursor.writeStringAscii(segment.value, size)

                    BytecodeStringEncoding.UTF16 ->
                        cursor.writeStringUtf16(segment.value, size)
                }
            }

            is DataSegment -> {
                cursor.writeCursor(segment.data.cursor())
            }
        }

        val actualSize = cursor.position - segmentStart
        val expectedSize = segment.size(stringEncoding)

        if (actualSize != expectedSize) {
            // Log and continue rather than crash — a size mismatch here typically indicates
            // incomplete normalization (e.g., variadic Pop with wrong arg count). The written
            // bytecode may be malformed but the save should not abort entirely.
            logger.warn {
                "Segment size mismatch: getSize()=$expectedSize but wrote $actualSize bytes " +
                    "(labels=${segment.labels}, type=${segment.type})."
            }
        }
    }

    return BytecodeAndLabelOffsets(buffer, labelOffsets)
}

private fun writeInlineArgs(
    cursor: BufferCursor,
    instruction: Instruction,
    stringEncoding: BytecodeStringEncoding,
) {
    val opcode = instruction.opcode

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

                when (stringEncoding) {
                    BytecodeStringEncoding.ASCII ->
                        cursor.writeStringAscii(str, str.length + 1)

                    BytecodeStringEncoding.UTF16 ->
                        cursor.writeStringUtf16(str, 2 * str.length + 2)
                }
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

private fun writePushInstructions(
    cursor: BufferCursor,
    instruction: Instruction,
    stringEncoding: BytecodeStringEncoding,
) {
    val opcode = instruction.opcode

    for (i in opcode.params.indices) {
        val paramType = opcode.params[i].type
        val args = instruction.getArgs(i)

        for (arg in args) {
            if (arg is IntArg && arg.isRegRef) {
                // arg_pushr
                cursor.writeByte(OP_ARG_PUSHR.code.toByte())
                cursor.writeByte(arg.value.toByte())
            } else when (paramType) {
                ByteType, is RegType, RegVarType -> {
                    // arg_pushb
                    cursor.writeByte(OP_ARG_PUSHB.code.toByte())
                    cursor.writeByte(arg.coerceInt().toByte())
                }

                ShortType, ILabelVarType, is LabelType -> {
                    // arg_pushw
                    cursor.writeByte(OP_ARG_PUSHW.code.toByte())
                    cursor.writeShort(arg.coerceInt().toShort())
                }

                IntType -> {
                    // arg_pushl
                    cursor.writeByte(OP_ARG_PUSHL.code.toByte())
                    cursor.writeInt(arg.coerceInt())
                }

                FloatType -> {
                    // arg_pushl (floats are pushed as int bits)
                    cursor.writeByte(OP_ARG_PUSHL.code.toByte())
                    cursor.writeInt(arg.coerceFloat().toRawBits())
                }

                StringType -> {
                    // arg_pushs
                    cursor.writeByte(OP_ARG_PUSHS.code.toByte())
                    val str = arg.coerceString()

                    when (stringEncoding) {
                        BytecodeStringEncoding.ASCII ->
                            cursor.writeStringAscii(str, str.length + 1)

                        BytecodeStringEncoding.UTF16 ->
                            cursor.writeStringUtf16(str, 2 * str.length + 2)
                    }
                }

                else -> error(
                    "Parameter type ${paramType::class.simpleName} not supported for push."
                )
            }
        }
    }
}

private data class PushInfo(
    val segmentIdx: Int,
    val instructionIdx: Int,
    val arg: Arg,
    val opcode: Opcode,
)

private data class PopNormalization(
    val segmentIdx: Int,
    val instructionIdx: Int,
    val args: List<Arg>,
    val pushesToRemove: List<PushInfo>,
)

private data class PushPopRelationships(
    val normalizations: List<PopNormalization>,
    val pushesToRemove: Set<Pair<Int, Int>>,
)

/**
 * Normalizes the bytecode IR by inlining stack arguments into Pop instructions.
 *
 * PSO bytecode uses a push/pop pattern: `arg_push*` instructions push values onto a virtual stack,
 * then a Pop opcode consumes them. This function removes the push instructions and places their
 * arguments directly on the consuming Pop instruction, producing a flat, platform-agnostic IR.
 *
 * This is called unconditionally during [parseBytecode] so the IR is always in normalized form.
 * Previously this was optional (controlled by an `inlineStackArgs` flag passed from the UI), but
 * since all downstream consumers (disassembler, assembler, data flow analysis) benefit from the
 * normalized form, it is now always applied at parse time. On write-back, push instructions are
 * regenerated from the inlined args.
 *
 * [IntArg.isRegRef] preserves `arg_pushr` semantics (register reference vs. literal value) so
 * no information is lost during normalization.
 */
private fun normalizeStackArgs(ir: BytecodeIr) {
    // Collect all instructions across all segments in order.
    val allSegments = ir.segments.filterIsInstance<InstructionSegment>()

    // First pass: identify push/pop relationships.
    val (normalizations, pushesToRemove) = identifyPushPopRelationships(allSegments)

    // Second pass: apply normalizations.
    applyNormalizations(allSegments, pushesToRemove, normalizations)

}

/**
 * First pass: identify push/pop relationships.
 * We track which push instructions to remove and which pop instructions to augment.
 */
private fun identifyPushPopRelationships(
    allSegments: List<InstructionSegment>,
): PushPopRelationships {
    val stack = mutableListOf<PushInfo>()
    var inVaBlock = false
    val normalizations = mutableListOf<PopNormalization>()
    // Track pushes to remove (by segment index and instruction index).
    val pushesToRemove = mutableSetOf<Pair<Int, Int>>()

    for (segIdx in allSegments.indices) {
        val segment = allSegments[segIdx]

        // Reset state at segment boundaries. Each segment is a separate function/label,
        // so pushes and va blocks from a previous segment should not carry over.
        stack.clear()
        inVaBlock = false

        for (instIdx in segment.instructions.indices) {
            val inst = segment.instructions[instIdx]
            val opcode = inst.opcode

            when {
                opcode.code == OP_VA_START.code -> {
                    inVaBlock = true
                }

                opcode.code == OP_VA_END.code -> {
                    inVaBlock = false
                    stack.clear()
                }

                inVaBlock -> {
                    // Inside va_start/va_end blocks, don't normalize.
                    if (opcode.stack == StackInteraction.Push) {
                        stack.add(PushInfo(segIdx, instIdx, inst.args.firstOrNull() ?: IntArg(0), opcode))
                    }
                }

                opcode.stack == StackInteraction.Push -> {
                    if (inst.args.isEmpty()) {
                        logger.warn { "Push instruction ${opcode.mnemonic} at seg=$segIdx inst=$instIdx has no args; defaulting to IntArg(0)" }
                    }
                    val arg = inst.args.firstOrNull() ?: IntArg(0)
                    stack.add(PushInfo(segIdx, instIdx, arg, opcode))
                }

                opcode.stack == StackInteraction.Pop -> {
                    // Variadic Pop opcodes (e.g. switch_jmp) consume all available stack entries;
                    // fixed Pop opcodes consume exactly paramCount entries.
                    val consumeCount = if (opcode.varargs) stack.size else opcode.params.size

                    if (consumeCount > 0 && stack.size >= consumeCount) {
                        // Consume args from stack (stack is LIFO, params are in forward order).
                        val consumed = stack.subList(stack.size - consumeCount, stack.size).toList()

                        val normalizedArgs = mutableListOf<Arg>()

                        for (j in consumed.indices) {
                            val pushInfo = consumed[j]
                            // For variadic params, reuse the last declared param type for extra args.
                            val paramType = opcode.params[j.coerceAtMost(opcode.params.lastIndex)].type
                            normalizedArgs.add(normalizeArg(pushInfo.arg, pushInfo.opcode, paramType))
                        }

                        normalizations.add(
                            PopNormalization(segIdx, instIdx, normalizedArgs, consumed)
                        )

                        for (p in consumed) {
                            pushesToRemove.add(Pair(p.segmentIdx, p.instructionIdx))
                        }

                        repeat(consumeCount) { stack.removeAt(stack.lastIndex) }
                    } else {
                        // Not enough pushes on the stack; skip normalization for this pop.
                        stack.clear()
                    }
                }

                // Unconditional control-flow terminates the current execution path.
                // Any pushes still on the stack are orphaned and must be discarded.
                opcode.code == OP_RET.code || opcode.code == OP_JMP.code -> {
                    stack.clear()
                }

                // Conditional branches and calls: the branch target may consume or push
                // different values, so any pushes accumulated so far cannot safely be
                // matched to a later pop on the fall-through path. Clear to avoid
                // mismatched normalization with hand-edited scripts.
                opcode.code == OP_JMP_ON.code ||
                opcode.code == OP_JMP_OFF.code ||
                opcode.code == OP_JMP_E.code ||
                opcode.code == OP_JMPI_E.code ||
                opcode.code == OP_JMP_NE.code ||
                opcode.code == OP_JMPI_NE.code ||
                opcode.code == OP_UJMP_G.code ||
                opcode.code == OP_UJMPI_G.code ||
                opcode.code == OP_JMP_G.code ||
                opcode.code == OP_JMPI_G.code ||
                opcode.code == OP_UJMP_L.code ||
                opcode.code == OP_UJMPI_L.code ||
                opcode.code == OP_JMP_L.code ||
                opcode.code == OP_JMPI_L.code ||
                opcode.code == OP_UJMP_GE.code ||
                opcode.code == OP_UJMPI_GE.code ||
                opcode.code == OP_JMP_GE.code ||
                opcode.code == OP_JMPI_GE.code ||
                opcode.code == OP_UJMP_LE.code ||
                opcode.code == OP_UJMPI_LE.code ||
                opcode.code == OP_JMP_LE.code ||
                opcode.code == OP_JMPI_LE.code ||
                opcode.code == OP_SWITCH_JMP.code ||
                opcode.code == OP_CALL.code ||
                opcode.code == OP_VA_CALL.code ||
                opcode.code == OP_SWITCH_CALL.code -> {
                    stack.clear()
                }

                else -> Unit
            }
        }
    }

    return PushPopRelationships(normalizations, pushesToRemove)
}

/**
 * Second pass: apply normalizations.
 * Removes push instructions and inlines their arguments on the corresponding Pop instructions.
 */
private fun applyNormalizations(
    allSegments: List<InstructionSegment>,
    pushesToRemove: Set<Pair<Int, Int>>,
    normalizations: List<PopNormalization>,
) {
    // Remove push instructions in reverse order to preserve indices.
    for (segIdx in allSegments.indices.reversed()) {
        val segment = allSegments[segIdx]

        for (instIdx in segment.instructions.indices.reversed()) {
            if (Pair(segIdx, instIdx) in pushesToRemove) {
                segment.instructions.removeAt(instIdx)
            }
        }
    }

    // Pre-compute per-segment sorted removal indices for O(n log n) adjustment.
    val removedBySegment: Map<Int, List<Int>> = pushesToRemove
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, indices) -> indices.sorted() }

    // Inline args on Pop instructions. Indices have shifted due to removals above,
    // so adjust each Pop's index by the number of pushes removed before it.
    for (norm in normalizations) {
        val segment = allSegments[norm.segmentIdx]
        val sortedRemovals = removedBySegment[norm.segmentIdx] ?: emptyList()
        // Binary search to count elements strictly less than norm.instructionIdx.
        val removedBefore = sortedRemovals.binarySearch(norm.instructionIdx)
            .let { if (it >= 0) it else -(it + 1) }
        val adjustedIdx = norm.instructionIdx - removedBefore

        if (adjustedIdx >= 0 && adjustedIdx < segment.instructions.size) {
            val oldInst = segment.instructions[adjustedIdx]

            if (oldInst.opcode.stack == StackInteraction.Pop) {
                segment.instructions[adjustedIdx] = Instruction(
                    oldInst.opcode,
                    norm.args,
                    oldInst.valid,
                    oldInst.srcLoc,
                )
            }
        }
    }
}

private fun normalizeArg(arg: Arg, pushOpcode: Opcode, paramType: AnyType): Arg {
    return when {
        // arg_pushr targeting a non-register parameter → mark as register reference.
        pushOpcode.code == OP_ARG_PUSHR.code && paramType !is RegType -> {
            IntArg(arg.coerceInt(), isRegRef = true)
        }
        // arg_pushl targeting a float parameter → reinterpret int bits as float.
        // arg_pushb/arg_pushw paths are kept for robustness; in practice PSO always uses
        // arg_pushl for floats. Byte/short values produce denormalized (near-zero) floats,
        // which is almost certainly a sign of malformed bytecode.
        (pushOpcode.code == OP_ARG_PUSHL.code ||
                pushOpcode.code == OP_ARG_PUSHB.code ||
                pushOpcode.code == OP_ARG_PUSHW.code) && paramType == FloatType -> {
            if (pushOpcode.code == OP_ARG_PUSHB.code || pushOpcode.code == OP_ARG_PUSHW.code) {
                logger.warn {
                    "Float parameter pushed via ${pushOpcode.mnemonic} (value=${arg.coerceInt()}); " +
                        "byte/short values produce denormalized floats. Malformed bytecode?"
                }
            }
            FloatArg(Float.fromBits(arg.coerceInt()))
        }
        else -> arg
    }
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
