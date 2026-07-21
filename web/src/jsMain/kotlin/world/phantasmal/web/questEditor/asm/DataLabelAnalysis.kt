package world.phantasmal.web.questEditor.asm

import mu.KotlinLogging
import world.phantasmal.psolib.asm.*

private val logger = KotlinLogging.logger {}

/**
 * Describes one `set_symbol_chat_collision` trigger extracted from bytecodeIr,
 * with its world-space position, activation radius, resolved per-slot stage
 * info, and the source line of the opcode call.
 */
data class SymbolChatTriggerInfo(
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float,
    val stages: List<SymbolChatTriggerStage>,
    /** Source line of the `set_symbol_chat_collision` mnemonic, 1-based; null if unavailable. */
    val lineNo: Int?,
)

/**
 * One spec slot of a `set_symbol_chat_collision` trigger, mirroring the dat-side
 * spec1/spec2/spec3 structure. Resolution rules:
 *
 * - When [dlabel] is non-null, the 60-byte body at that data label wins and
 *   [scId] is ignored (newserv `Map.cc:1019`).
 * - Else when [scId] is in [0, ENTRY_COUNT), it indexes the built-in
 *   `symbolchatcolli.prs` table.
 * - Otherwise the slot is "hidden / sentinel" and renders as empty.
 *
 * All three fields may be null if the analyser couldn't back-trace the
 * corresponding register write from the opcode call.
 */
data class SymbolChatTriggerStage(
    /** 1, 2, or 3. */
    val slot: Int,
    /** Low 16 bits of the spec u32 at R+3+slot. */
    val scId: Int?,
    /** High 16 bits of the spec u32 at R+3+slot. */
    val switchFlag: Int?,
    /** dlabel from R+6+slot — non-null means custom HEX data wins. */
    val dlabel: Int?,
)

enum class DataLabelType {
    NpcData,
    PhysicalData,
    AttackData,
    ResistData,
    MovementData,
    /** Generic image / binary blob — referenced by [call_image_data] (0xf8ee). */
    ImageData,
    /** Float array — set manually by the user (no opcode-based detection). */
    FloatData,
    /**
     * Vector list — array of (x,y,z,duration) Float32 records (16 bytes each),
     * referenced by `get_vector_from_path` (0xf8db) and
     * `compute_bezier_curve_point` (0xf8f2) as their final dlabel arg.
     */
    VectorData,
    /** Symbol chat blob — set manually by the user (no opcode-based detection). */
    SymbolChatData,
    /**
     * Symbol chat HEX block referenced by `set_symbol_chat_collision`
     * (opcode 0xF8A6 — qedit calls it `symbol_chat_create`). The opcode
     * reads a fixed block of 10 registers; R+7..R+9 hold up to 3 dlabel
     * references for the symbol chat data records, which are detected
     * automatically by back-tracing `leti` writes within the same segment.
     * Layout differs from [SymbolChatData]: each record begins with face
     * type / face colour / sound effect bytes (qedit.info wiki).
     */
    SymbolChatHexData,
}

/**
 * Scans all instruction segments in the bytecodeIr and maps dlabel arguments of known
 * data-referencing opcodes to their [DataLabelType].
 */
fun analyzeDataLabels(bytecodeIr: BytecodeIr): Map<Int, DataLabelType> {
    val opcodeNpcData = codeToOpcode(0xF841)       // get_npc_data
    val opcodePhysicalData = codeToOpcode(0xF892)   // get_physical_data
    val opcodeAttackData = codeToOpcode(0xF893)     // get_attack_data
    val opcodeResistData = codeToOpcode(0xF894)     // get_resist_data
    val opcodeMovementData = codeToOpcode(0xF895)   // get_movement_data
    val opcodeCallImageData = codeToOpcode(0xF8EE)  // call_image_data
    val opcodeGetVectorFromPath = codeToOpcode(0xF8DB)       // get_vector_from_path
    val opcodeComputeBezierCurvePoint = codeToOpcode(0xF8F2) // compute_bezier_curve_point (load_unk_data)
    val opcodeSetSymbolChatCollision = codeToOpcode(0xF8A6)  // set_symbol_chat_collision (a.k.a. symbol_chat_create)
    val opcodeLeti = codeToOpcode(0x09)             // leti R, immediate

    val result = mutableMapOf<Int, DataLabelType>()

    for (segment in bytecodeIr.segments) {
        if (segment !is InstructionSegment) continue

        segment.instructions.forEachIndexed { instIdx, instruction ->
            // For call_image_data the dlabel is the SECOND argument (0xf8ee
            // signature: T_ARGS, T_DWORD, T_DATA). For all the other typed
            // ops we know the dlabel is the first (and only) argument.
            val (type, labelArgIndex) = when (instruction.opcode) {
                opcodeNpcData -> DataLabelType.NpcData to 0
                opcodePhysicalData -> DataLabelType.PhysicalData to 0
                opcodeAttackData -> DataLabelType.AttackData to 0
                opcodeResistData -> DataLabelType.ResistData to 0
                opcodeMovementData -> DataLabelType.MovementData to 0
                opcodeCallImageData -> DataLabelType.ImageData to 1
                // Both vector ops have signature (int, int, int, int, reg, dlabel) — last arg.
                opcodeGetVectorFromPath -> DataLabelType.VectorData to 5
                opcodeComputeBezierCurvePoint -> DataLabelType.VectorData to 5
                opcodeSetSymbolChatCollision -> {
                    // The dlabels for the 3 symbol chats live in R+7..R+9
                    // of the 10-register block whose base is this opcode's
                    // single arg. Back-trace `leti` writes earlier in the
                    // same segment to recover them, and mark each as
                    // SymbolChatHexData.
                    val baseReg = instruction.args.getOrNull(0)?.coerceInt() ?: return@forEachIndexed
                    for (slot in 7..9) {
                        val targetReg = baseReg + slot
                        // Walk backward to find the most recent leti writing
                        // to targetReg within this segment.
                        for (j in (instIdx - 1) downTo 0) {
                            val prev = segment.instructions[j]
                            if (prev.opcode !== opcodeLeti) continue
                            val writeReg = prev.args.getOrNull(0)?.coerceInt() ?: continue
                            if (writeReg != targetReg) continue
                            val labelId = prev.args.getOrNull(1)?.coerceInt() ?: break
                            // Don't overwrite a stricter type set by another opcode.
                            if (labelId !in result) result[labelId] = DataLabelType.SymbolChatHexData
                            break
                        }
                    }
                    return@forEachIndexed
                }
                else -> return@forEachIndexed
            }

            val labelId = instruction.args.getOrNull(labelArgIndex)?.coerceInt() ?: return@forEachIndexed
            result[labelId] = type
        }
    }

    return result
}

/**
 * Scans all instruction segments for `set_symbol_chat_collision` (0xF8A6)
 * and back-traces the 10 registers the opcode reads to produce one
 * [SymbolChatTriggerInfo] per call:
 *
 * - **R+0..R+3** — world-space X/Y/Z and radius (floats; accept both
 *   `fleti` and `leti`-with-IEEE-754-bit-pattern since PSO scripts use
 *   either).
 * - **R+4..R+6** — per-slot spec u32 packed as `(switch_flag:hi16, sc_id:lo16)`.
 * - **R+7..R+9** — per-slot dlabel (non-null dlabel wins over the spec's
 *   built-in SC ID per newserv `Map.cc:1019`).
 * - `srcLoc.mnemonic.lineNo` of the opcode (when populated — only for
 *   bytecode assembled from source text, not for binaries parsed from
 *   .qst files).
 *
 * The back-trace is control-flow–unaware: it walks the instruction list
 * linearly and picks the most recent write to each target register without
 * regard for branches. This matches the emit pattern qedit and
 * newserv-compiled scripts use in practice (immediates set in source
 * order immediately before the opcode) but can produce phantom values on
 * hand-written assembly that places `leti`/`fleti` inside conditional
 * branches or in earlier segments. When a coord register can't be resolved,
 * the trigger is dropped and a count of encountered-vs-dropped calls is
 * logged at WARN so regressions in representative quests are visible in
 * the console.
 */
fun analyzeSymbolChatTriggers(bytecodeIr: BytecodeIr): List<SymbolChatTriggerInfo> {
    val opcodeSetSymbolChatCollision = codeToOpcode(0xF8A6)
    val opcodeLeti = codeToOpcode(0x09)   // leti R, int_immediate
    val opcodeFleti = codeToOpcode(0xF904) // fleti R, float_immediate

    val result = mutableListOf<SymbolChatTriggerInfo>()
    var encountered = 0
    var dropped = 0

    for (segment in bytecodeIr.segments) {
        if (segment !is InstructionSegment) continue

        segment.instructions.forEachIndexed { instIdx, instruction ->
            if (instruction.opcode !== opcodeSetSymbolChatCollision) return@forEachIndexed
            encountered++

            val baseReg = instruction.args.getOrNull(0)?.coerceInt()
            if (baseReg == null) {
                dropped++
                return@forEachIndexed
            }

            // Back-trace float registers R+0..R+3 (X, Y, Z, radius).
            // Accept both `fleti` (float immediate) and `leti` (int immediate)
            // because PSO scripts sometimes use `leti` with an IEEE 754 bit
            // pattern to set float registers; `coerceFloat()` handles the
            // bit-reinterpretation correctly for both arg types.
            val floats = FloatArray(4) { Float.NaN }
            for (slot in 0..3) {
                val targetReg = baseReg + slot
                for (j in (instIdx - 1) downTo 0) {
                    val prev = segment.instructions[j]
                    if (prev.opcode !== opcodeFleti && prev.opcode !== opcodeLeti) continue
                    val writeReg = prev.args.getOrNull(0)?.coerceInt() ?: continue
                    if (writeReg != targetReg) continue
                    floats[slot] = prev.args.getOrNull(1)?.coerceFloat() ?: break
                    break
                }
            }

            // Skip if any coordinate couldn't be resolved. Usually means the
            // script uses a pattern the back-trace doesn't understand
            // (cross-segment init, copied registers, conditional branches).
            if (floats.any { it.isNaN() }) {
                dropped++
                return@forEachIndexed
            }

            // Back-trace spec registers R+4..R+6. Each spec is a u32 packed as
            // (switch_flag:high_u16, sc_id:low_u16). `leti` writes a signed
            // int whose bit pattern carries both halves.
            val specValues = arrayOfNulls<Int>(3)
            for (slot in 0..2) {
                val targetReg = baseReg + 4 + slot
                for (j in (instIdx - 1) downTo 0) {
                    val prev = segment.instructions[j]
                    if (prev.opcode !== opcodeLeti) continue
                    val writeReg = prev.args.getOrNull(0)?.coerceInt() ?: continue
                    if (writeReg != targetReg) continue
                    specValues[slot] = prev.args.getOrNull(1)?.coerceInt() ?: break
                    break
                }
            }

            // Back-trace dlabel registers R+7..R+9.
            val dlabels = arrayOfNulls<Int>(3)
            for (slot in 0..2) {
                val targetReg = baseReg + 7 + slot
                for (j in (instIdx - 1) downTo 0) {
                    val prev = segment.instructions[j]
                    if (prev.opcode !== opcodeLeti) continue
                    val writeReg = prev.args.getOrNull(0)?.coerceInt() ?: continue
                    if (writeReg != targetReg) continue
                    dlabels[slot] = prev.args.getOrNull(1)?.coerceInt() ?: break
                    break
                }
            }

            val stages = (0..2).map { slot ->
                val spec = specValues[slot]
                SymbolChatTriggerStage(
                    slot = slot + 1,
                    scId = spec?.let { it and 0xFFFF },
                    switchFlag = spec?.let { (it ushr 16) and 0xFFFF },
                    dlabel = dlabels[slot],
                )
            }

            result.add(SymbolChatTriggerInfo(
                x = floats[0], y = floats[1], z = floats[2], radius = floats[3],
                stages = stages,
                lineNo = instruction.srcLoc?.mnemonic?.lineNo,
            ))
        }
    }

    if (dropped > 0) {
        logger.warn {
            "analyzeSymbolChatTriggers: dropped $dropped of $encountered " +
                "set_symbol_chat_collision call(s) because their world-space " +
                "coordinates couldn't be back-traced. Expect fewer trigger " +
                "rings in the 3D viewport than opcode calls in the script."
        }
    }

    return result
}
