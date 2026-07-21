package world.phantasmal.psolib.asm.dataFlowAnalysis

import mu.KotlinLogging
import world.phantasmal.psolib.asm.Instruction
import world.phantasmal.psolib.asm.InstructionSegment
import world.phantasmal.psolib.asm.IntArg
import world.phantasmal.psolib.asm.OP_AT_COORDS_CALL
import world.phantasmal.psolib.asm.OP_AT_COORDS_CALL_EX
import world.phantasmal.psolib.asm.OP_AT_COORDS_TALK
import world.phantasmal.psolib.asm.OP_AT_COORDS_TALK_EX
import world.phantasmal.psolib.asm.OP_COL_NPCIN
import world.phantasmal.psolib.asm.OP_COL_NPCINR
import world.phantasmal.psolib.asm.OP_COL_PLINAW
import world.phantasmal.psolib.asm.OP_CLR_FLOOR_HANDLER
import world.phantasmal.psolib.asm.OP_CLR_QT_CANCEL
import world.phantasmal.psolib.asm.OP_CLR_QT_EXIT
import world.phantasmal.psolib.asm.OP_CLR_QT_FAILURE
import world.phantasmal.psolib.asm.OP_CLR_QT_SUCCESS
import world.phantasmal.psolib.asm.OP_CLEAR_QUEST_BOARD_HANDLER
import world.phantasmal.psolib.asm.OP_GET_FLOOR_NUMBER
import world.phantasmal.psolib.asm.OP_JMPI_E
import world.phantasmal.psolib.asm.OP_JMPI_NE
import world.phantasmal.psolib.asm.OP_LET
import world.phantasmal.psolib.asm.OP_LETI
import world.phantasmal.psolib.asm.OP_NPC_CHECK_STRAGGLE_EX
import world.phantasmal.psolib.asm.OP_NPC_COORDS_CALL_EX
import world.phantasmal.psolib.asm.OP_PARTICLE2
import world.phantasmal.psolib.asm.OP_PARTICLE_EFFECT_NC
import world.phantasmal.psolib.asm.OP_PARTICLE_ID_V3
import world.phantasmal.psolib.asm.OP_PARTICLE_V3
import world.phantasmal.psolib.asm.OP_PLAYER_EFFECT_NC
import world.phantasmal.psolib.asm.OP_PARTY_COORDS_CALL_EX
import world.phantasmal.psolib.asm.OP_SET_CHAT_CALLBACK
import world.phantasmal.psolib.asm.OP_SET_CHAT_CALLBACK_NO_FILTER
import world.phantasmal.psolib.asm.OP_SET_FLOOR_HANDLER
import world.phantasmal.psolib.asm.OP_SET_OBJ_PARAM
import world.phantasmal.psolib.asm.OP_SET_OBJ_PARAM_EX
import world.phantasmal.psolib.asm.OP_SET_QT_CANCEL
import world.phantasmal.psolib.asm.OP_SET_QT_EXIT
import world.phantasmal.psolib.asm.OP_SET_QT_FAILURE
import world.phantasmal.psolib.asm.OP_SET_QT_SUCCESS
import world.phantasmal.psolib.asm.OP_SET_QUEST_BOARD_HANDLER
import world.phantasmal.psolib.asm.OP_SWITCH_CALL
import world.phantasmal.psolib.asm.OP_SWITCH_JMP
import world.phantasmal.psolib.asm.OP_THREAD
import world.phantasmal.psolib.asm.OP_THREAD_STG
import world.phantasmal.psolib.asm.RegType

private val logger = KotlinLogging.logger {}

/**
 * The client-side origin of a quest particle emitter.
 */
sealed class ParticleSpawnOrigin {
    /** A fixed world-space position. Quest registers are converted numerically from int to float. */
    data class WorldPosition(val x: Int, val y: Int, val z: Int) : ParticleSpawnOrigin()

    /**
     * An entity resolved by the client's global entity ID table. Opcode sources resolve it when
     * executed; DAT sources remain attached to their map object. The Y offset is added to the
     * entity's position; X and Z are unchanged.
     */
    data class EntityPosition(val entityId: Int, val yOffset: Int) : ParticleSpawnOrigin()
}

/** The quest opcode variant that creates the emitter. */
enum class ParticleSpawnOpcode {
    ParticleV3,
    Particle2,
    ParticleIdV3,
    ParticleEffectNoCull,
    PlayerEffectNoCull,
}

/** The quest resource that creates a particle emitter. */
sealed class ParticleSpawnSource {
    /** A BIN quest-script opcode invocation. */
    data class Opcode(val opcode: ParticleSpawnOpcode) : ParticleSpawnSource()

    /** A DAT map object whose client constructor creates a persistent emitter. */
    data class DatObject(val objectTypeId: Int, val objectId: Int) : ParticleSpawnSource()
}

/**
 * A quest particle emitter created by a DAT object or a statically resolved BIN opcode.
 *
 * PSOBB does not copy a floor ID into the runtime emitter. DAT objects inherit their DAT floor;
 * [executionFloorIds] records the logical floors whose script paths can execute a BIN opcode.
 * On a floor transition PSOBB destroys the old floor's emitters before starting the new floor's
 * handler. For persistent threads that resume after a yield, [executionFloorIds] is derived by
 * analyzing the continuation against every possible runtime `g_CurrentFloor` value.
 *
 * @property origin Fixed coordinates or an entity reference resolved by the editor.
 * @property particleId Particle effect ID; index into `particleentry.dat` (or area-specific
 *   `particleentryaXX.dat` for IDs 512 through 575).
 * @property lifetimeFrames Number of frames an opcode emitter should last, or null for a
 *   persistent DAT particle object.
 * @property source The BIN opcode or DAT object that creates the emitter.
 * @property hasExtendedDrawRange Whether the source selects the client's extended/no-cull
 *   particle draw behavior.
 * @property executionFloorIds Logical floors whose handler paths can execute this invocation.
 */
data class ParticleSpawn(
    val origin: ParticleSpawnOrigin,
    val particleId: Int,
    val lifetimeFrames: Int?,
    val source: ParticleSpawnSource,
    val hasExtendedDrawRange: Boolean,
    val executionFloorIds: Set<Int> = emptySet(),
)

/**
 * Scans reachable uses of all five PSOBB quest particle opcodes and resolves their register
 * arguments via constant propagation on the [ControlFlowGraph].
 *
 * Invocations whose arguments can't be reduced to a single value are skipped (logged at debug
 * level).
 */
fun getParticleSpawns(
    instructionSegments: List<InstructionSegment>,
    entityEntryPointFloorIds: Map<Int, Set<Int>> = emptyMap(),
    createCfg: () -> ControlFlowGraph,
): List<ParticleSpawn> {
    val spawns = mutableListOf<ParticleSpawn>()
    var cfg: ControlFlowGraph? = null
    var executionFloors: ParticleExecutionFloors? = null

    for (segment in instructionSegments) {
        for (inst in segment.instructions) {
            val opcode = when (inst.opcode.code) {
                OP_PARTICLE_V3.code -> ParticleSpawnOpcode.ParticleV3
                OP_PARTICLE2.code -> ParticleSpawnOpcode.Particle2
                OP_PARTICLE_ID_V3.code -> ParticleSpawnOpcode.ParticleIdV3
                OP_PARTICLE_EFFECT_NC.code -> ParticleSpawnOpcode.ParticleEffectNoCull
                OP_PLAYER_EFFECT_NC.code -> ParticleSpawnOpcode.PlayerEffectNoCull
                else -> continue
            }

            if (cfg == null) cfg = createCfg()
            if (executionFloors == null) {
                executionFloors = computeParticleExecutionFloors(
                    cfg,
                    instructionSegments,
                    entityEntryPointFloorIds,
                )
            }

            val firstReg = (inst.args.firstOrNull() as? IntArg)?.value ?: continue
            val registerCount = when (opcode) {
                ParticleSpawnOpcode.ParticleV3,
                ParticleSpawnOpcode.ParticleEffectNoCull -> 5
                ParticleSpawnOpcode.Particle2 -> 3
                ParticleSpawnOpcode.ParticleIdV3,
                ParticleSpawnOpcode.PlayerEffectNoCull -> 4
            }
            if (firstReg !in 0..(256 - registerCount)) continue

            val values = (0 until registerCount).map { offset ->
                val value = getRegisterValue(cfg, inst, firstReg + offset)
                if (value.size == 1L) value[0] else null
            }
            if (values.any { it == null }) {
                logger.debug {
                    "Couldn't determine constant arguments for ${inst.opcode.mnemonic} in segment with " +
                            "labels ${segment.labels}."
                }
                continue
            }

            val executionFloorIds = executionFloors.floorsByInstruction[inst] ?: emptySet()
            // A marker requires a real client entry path. Bytes after a ret and unreferenced
            // labels never execute. Persistent threads are analyzed again for all 18 possible
            // runtime floors after each resumable yield.
            if (executionFloorIds.isEmpty()) continue

            val spawn = when (opcode) {
                ParticleSpawnOpcode.ParticleV3,
                ParticleSpawnOpcode.ParticleEffectNoCull -> ParticleSpawn(
                    origin = ParticleSpawnOrigin.WorldPosition(values[0]!!, values[1]!!, values[2]!!),
                    particleId = values[3]!!.toShort().toInt(),
                    lifetimeFrames = values[4]!!,
                    source = ParticleSpawnSource.Opcode(opcode),
                    hasExtendedDrawRange = opcode == ParticleSpawnOpcode.ParticleEffectNoCull,
                    executionFloorIds = executionFloorIds,
                )
                ParticleSpawnOpcode.Particle2 -> ParticleSpawn(
                    origin = ParticleSpawnOrigin.WorldPosition(values[0]!!, values[1]!!, values[2]!!),
                    particleId = inst.args.getOrNull(1)?.coerceInt() ?: continue,
                    // The client temporarily selects the x87 round-toward-zero mode here.
                    lifetimeFrames = inst.args.getOrNull(2)?.coerceFloat()?.toInt() ?: continue,
                    source = ParticleSpawnSource.Opcode(opcode),
                    // PSOBB's particle2 handler always ORs flag 0x40 into the emitter.
                    hasExtendedDrawRange = true,
                    executionFloorIds = executionFloorIds,
                )
                ParticleSpawnOpcode.ParticleIdV3,
                ParticleSpawnOpcode.PlayerEffectNoCull -> ParticleSpawn(
                    origin = ParticleSpawnOrigin.EntityPosition(
                        entityId = values[2]!! and 0xFFFF,
                        yOffset = values[3]!!,
                    ),
                    particleId = values[0]!!.toShort().toInt(),
                    lifetimeFrames = values[1]!!,
                    source = ParticleSpawnSource.Opcode(opcode),
                    hasExtendedDrawRange = opcode == ParticleSpawnOpcode.PlayerEffectNoCull,
                    executionFloorIds = executionFloorIds,
                )
            }

            spawns.add(spawn)
        }
    }

    return spawns
}

/**
 * Returns a mapping from each client-reachable instruction to the logical floors on which the
 * client can execute it.
 *
 * The analysis is **path-sensitive on the floor register**. Each floor handler entry is BFSed
 * once per floor it is registered for; during the walk we track the set of registers known to
 * hold the current floor value (introduced via [OP_GET_FLOOR_NUMBER] and propagated through
 * [OP_LET]). When a [OP_SWITCH_JMP] / [OP_SWITCH_CALL] / [OP_JMPI_E] / [OP_JMPI_NE] terminator
 * dispatches on one of those registers, only the branch consistent with the BFS instance's
 * current floor is followed. This is what makes shared handlers like
 *
 * ```
 * set_floor_handler 1, 152
 * set_floor_handler 2, 152
 * ...
 * set_floor_handler 12, 152
 * 152:
 *   get_floor_number r250, r28
 *   let r53, r28
 *   switch_call r53, ..., L21, L22, L23, ..., L1032
 * ```
 *
 * resolve to per-floor attribution instead of "this code can be reached on floors 1..12, so
 * tag everything with all twelve."
 *
 * Reachability also follows callback-registration edges where the callback body is associated
 * with the registration floor:
 * - Spatial trigger/object registrations — their trigger geometry or interactable object is
 *   created on the current floor, so the registered label only fires on that floor. This
 *   includes `at_coords_*`, NPC/party-coordinate triggers, `set_obj_param`, NPC-straggle
 *   triggers, chat-sensor regions, and their `_ex` variants.
 * - [OP_THREAD_STG] — PSOBB reparents this thread to `g_QuestThreadListHead`, which is destroyed
 *   during a floor transition. An ordinary `thread` initially executes on its launch floor, but
 *   remains parented to the Quest object; after a resumable yield its continuation is analyzed
 *   against every possible runtime floor.
 *
 * Quest completion/cancel and Quest Board handlers are seeded separately as floor 0 because the
 * client invokes them at the Hunter's Guild / quest board on Pioneer 2, independently of the floor
 * on which they were registered.
 *
 * Label 0 is executed once while the quest starts on Pioneer 2 (logical floor 0), before the
 * current floor handler is started, so its reachable blocks are seeded as floor 0. Blocks not
 * reachable from a client entry point are absent from the map and never create an emitter.
 */
private data class ParticleExecutionFloors(
    val floorsByInstruction: Map<Instruction, Set<Int>>,
)

private data class ExecutionPoint(
    val block: BasicBlock,
    val instructionIndex: Int,
    val floor: Int,
    val floorBound: Boolean,
)

private data class TraversalState(
    val block: BasicBlock,
    val instructionIndex: Int,
    val floorRegisters: Set<Int>,
    val floorBound: Boolean,
    val floorHandlerWrites: Map<Int, Set<Int>>,
    val callbackHandlerWrites: Map<CallbackHandlerSlot, Set<Int>>,
)

private data class ThreadExitState(
    val floorRegisters: Set<Int>,
    val floorHandlerWrites: Map<Int, Set<Int>>,
    val callbackHandlerWrites: Map<CallbackHandlerSlot, Set<Int>>,
)

private enum class CallbackHandlerKind {
    QuestFailure,
    QuestSuccess,
    QuestCancel,
    QuestExit,
    QuestBoard,
}

private data class CallbackHandlerSlot(
    val kind: CallbackHandlerKind,
    val index: Int = 0,
)

private fun computeParticleExecutionFloors(
    cfg: ControlFlowGraph,
    instructionSegments: List<InstructionSegment>,
    entityEntryPointFloorIds: Map<Int, Set<Int>>,
): ParticleExecutionFloors {
    // Step 1: build label -> entry block (the first block of the segment carrying that label).
    val labelToEntryBlock = mutableMapOf<Int, BasicBlock>()
    for (block in cfg.blocks) {
        // The first block of a segment starts at index 0 and is the only one whose labels are
        // the segment's labels.
        if (block.start != 0) continue
        for (label in block.segment.labels) {
            labelToEntryBlock[label] = block
        }
    }

    // Pre-compute next-in-segment lookup. cfg.blocks is in segment-traversal order, so the
    // following block in the global list is the segment-sequential successor — but only when
    // it shares the same segment.
    val nextInSegment = mutableMapOf<BasicBlock, BasicBlock>()
    for (i in cfg.blocks.indices) {
        val b = cfg.blocks[i]
        val n = cfg.blocks.getOrNull(i + 1) ?: continue
        if (n.segment === b.segment) nextInSegment[b] = n
    }

    // Step 2: pre-compute callback edges. For each block that registers a callback whose
    // body is associated with the registration floor (spatial triggers/objects and floor-scoped
    // threads),
    // resolve the target label and record an extra edge
    // `block -> labelEntry` for the BFS to follow.
    val callbackEdges = mutableMapOf<Instruction, MutableList<BasicBlock>>()
    val ordinaryThreadEdges = mutableMapOf<Instruction, MutableList<BasicBlock>>()
    for (block in cfg.blocks) {
        for (i in block.start until block.end) {
            val inst = block.segment.instructions[i]
            val callbackLabelOffset = spatialCallbackLabelRegisterOffset(inst.opcode.code)
            if (callbackLabelOffset != null) {
                // The callback label is embedded in a register group instead of being an
                // inline ILabel operand, so the ordinary CFG cannot discover this edge.
                val firstReg = (inst.args.getOrNull(0) as? IntArg)?.value ?: continue
                if (firstReg !in 0 until (256 - callbackLabelOffset)) continue
                val labelValues = getRegisterValue(cfg, inst, firstReg + callbackLabelOffset)
                if (labelValues.size != 1L) continue
                val label = labelValues[0] ?: continue
                val target = labelToEntryBlock[label] ?: continue
                callbackEdges.getOrPut(inst) { mutableListOf() }.add(target)
                continue
            }

            when (inst.opcode.code) {
                OP_THREAD_STG.code -> {
                    // Single inline ILabelType arg.
                    val label = (inst.args.getOrNull(0) as? IntArg)?.value ?: continue
                    val target = labelToEntryBlock[label] ?: continue
                    callbackEdges.getOrPut(inst) { mutableListOf() }.add(target)
                }
                OP_THREAD.code -> {
                    val label = (inst.args.getOrNull(0) as? IntArg)?.value ?: continue
                    val target = labelToEntryBlock[label] ?: continue
                    ordinaryThreadEdges.getOrPut(inst) { mutableListOf() }.add(target)
                }
            }
        }
    }

    // Step 3: forward, path-sensitive, **interprocedural** reachability from actual client
    // entry points. New roots are discovered only from reachable registration instructions.
    // Floor-handler writes are carried through calls and branches, then committed when the
    // client thread yields or returns; this preserves the client's overwrite/clear semantics.
    // Merely having a setter in dead bytecode therefore does not register anything.
    //
    // Edge model — we deliberately do NOT use `block.to` directly because the CFG, via
    // linkReturningBlocks, gives every callee's Return block edges to *all* of its callers'
    // after-call blocks. Following those would let BFS from one floor handler walk into
    // another floor handler's after-call code through a shared helper, polluting the floor
    // tag for every spawn downstream. So:
    // - Return blocks contribute no forward edges within the BFS body. Instead, the floor
    //   register state at every Return block reached during a callee walk is *unioned* into
    //   the callee's "exit state". The exit state is then propagated to the caller's
    //   after-call block (segment-sequential successor of the call) — not the call-site
    //   pre-call state. This is what makes "helper sets r52 = r53; ret" propagate to the
    //   caller's switch_jmp(r52) and let path-sensitivity prune it.
    // - Call blocks recurse into callee entries; the recursive call returns the merged exit
    //   state which becomes the after-call block's incoming state.
    // - All other block types use `block.to` (filtered by branch pruning when the terminator
    //   dispatches on a floor-tracking register).
    //
    // Visited keys contain the block position, floor-register lineage, thread lifetime, and
    // pending floor-handler writes. To prevent infinite recursion on mutually-recursive callees,
    // in-progress contexts break cycles by returning their entry state. Per-instance exit-state
    // memoization keeps the recursion linear in distinct traversal states.
    val result = mutableMapOf<Instruction, MutableSet<Int>>()
    val pendingEntries = ArrayDeque<ExecutionPoint>()
    val discoveredEntries = mutableSetOf<ExecutionPoint>()

    fun enqueueEntry(label: Int, floor: Int, floorBound: Boolean) {
        val block = labelToEntryBlock[label] ?: return
        val entry = ExecutionPoint(block, block.start, floor, floorBound)
        if (discoveredEntries.add(entry)) {
            pendingEntries.add(entry)
        }
    }

    fun enqueueResume(block: BasicBlock, instructionIndex: Int) {
        // A persistent QuestThread2 resumes against whatever g_CurrentFloor is at that time.
        // The client supports exactly 18 logical floor slots.
        for (floor in 0 until 0x12) {
            val entry = ExecutionPoint(block, instructionIndex, floor, floorBound = false)
            if (discoveredEntries.add(entry)) pendingEntries.add(entry)
        }
    }

    fun commitFloorHandlerWrites(writes: Map<Int, Set<Int>>) {
        for ((floor, labels) in writes) {
            for (label in labels) enqueueEntry(label, floor, floorBound = false)
        }
    }

    fun commitCallbackHandlerWrites(writes: Map<CallbackHandlerSlot, Set<Int>>) {
        for ((slot, labels) in writes) {
            for (label in labels) {
                when (slot.kind) {
                    CallbackHandlerKind.QuestFailure,
                    CallbackHandlerKind.QuestSuccess,
                    CallbackHandlerKind.QuestCancel,
                    -> enqueueEntry(label, 0, floorBound = false)
                    CallbackHandlerKind.QuestBoard -> enqueueEntry(label, 0, floorBound = true)
                    CallbackHandlerKind.QuestExit -> {
                        for (floor in 0 until 0x12) {
                            enqueueEntry(label, floor, floorBound = true)
                        }
                    }
                }
            }
        }
    }

    fun <K> mergeHandlerWrites(
        left: Map<K, Set<Int>>,
        right: Map<K, Set<Int>>,
    ): Map<K, Set<Int>> {
        if (left.isEmpty()) return right
        if (right.isEmpty()) return left
        val merged = left.toMutableMap()
        for ((floor, labels) in right) {
            merged[floor] = merged[floor].orEmpty() + labels
        }
        return merged
    }

    // PSOBB constructs label 0 while the quest starts on Pioneer 2.
    enqueueEntry(0, 0, floorBound = false)
    for ((label, floorIds) in entityEntryPointFloorIds) {
        for (floor in floorIds) enqueueEntry(label, floor, floorBound = true)
    }

    while (pendingEntries.isNotEmpty()) {
        val (entryBlock, entryInstruction, currentFloor, entryFloorBound) = pendingEntries.removeFirst()
        val visited = mutableSetOf<TraversalState>()
        val exitStateCache = mutableMapOf<TraversalState, ThreadExitState>()
        val inProgress = mutableSetOf<TraversalState>()

        // Recursive BFS that returns the merged register state at all Return blocks
        // reached from `start` with `startRegs`. The depth limit guards against
        // pathological call chains; PSO scripts rarely exceed ~20 levels.
        fun bfs(
            start: BasicBlock,
            firstInstruction: Int,
            startRegs: Set<Int>,
            depth: Int,
            floorBound: Boolean,
            startFloorHandlerWrites: Map<Int, Set<Int>> = emptyMap(),
            startCallbackHandlerWrites: Map<CallbackHandlerSlot, Set<Int>> = emptyMap(),
        ): ThreadExitState? {
                val key = TraversalState(
                    start,
                    firstInstruction,
                    startRegs,
                    floorBound,
                    startFloorHandlerWrites,
                    startCallbackHandlerWrites,
                )
                exitStateCache[key]?.let { return it }
                if (key in inProgress || depth >= 64) {
                    return ThreadExitState(
                        startRegs, startFloorHandlerWrites, startCallbackHandlerWrites,
                    )
                }
                inProgress.add(key)

                val queue = ArrayDeque<TraversalState>()
                queue.add(key)
                var mergedExit: ThreadExitState? = null
                var suspended = false

                while (queue.isNotEmpty()) {
                    val (b, begin, regs, _, incomingFloorHandlerWrites, incomingCallbackHandlerWrites) =
                        queue.removeFirst()
                    val visitKey = TraversalState(
                        b,
                        begin,
                        regs,
                        floorBound,
                        incomingFloorHandlerWrites,
                        incomingCallbackHandlerWrites,
                    )
                    if (!visited.add(visitKey)) continue
                    // Registration opcodes mutate client handler slots only if control flow
                    // reaches them. Writes remain pending until this VM execution slice ends.
                    var yielded = false
                    var floorHandlerWrites = incomingFloorHandlerWrites
                    var callbackHandlerWrites = incomingCallbackHandlerWrites
                    for (i in begin until b.end) {
                        val inst = b.segment.instructions[i]
                        result.getOrPut(inst) { mutableSetOf() }.add(currentFloor)
                        when (inst.opcode.code) {
                            OP_SET_FLOOR_HANDLER.code -> {
                                val floor = (inst.args.getOrNull(0) as? IntArg)?.value ?: continue
                                val label = (inst.args.getOrNull(1) as? IntArg)?.value ?: continue
                                // PSOBB's set_floor_handler accepts only logical floors 0..17.
                                if (floor in 0 until 0x12) {
                                    floorHandlerWrites = floorHandlerWrites + (floor to setOf(label))
                                }
                            }
                            OP_CLR_FLOOR_HANDLER.code -> {
                                val floor = (inst.args.getOrNull(0) as? IntArg)?.value ?: continue
                                if (floor in 0 until 0x12) {
                                    floorHandlerWrites = floorHandlerWrites + (floor to emptySet())
                                }
                            }
                            OP_SET_QT_FAILURE.code,
                            OP_SET_QT_SUCCESS.code,
                            OP_SET_QT_CANCEL.code,
                            OP_SET_QT_EXIT.code,
                            -> {
                                val label = (inst.args.getOrNull(0) as? IntArg)?.value ?: continue
                                val kind = when (inst.opcode.code) {
                                    OP_SET_QT_FAILURE.code -> CallbackHandlerKind.QuestFailure
                                    OP_SET_QT_SUCCESS.code -> CallbackHandlerKind.QuestSuccess
                                    OP_SET_QT_CANCEL.code -> CallbackHandlerKind.QuestCancel
                                    else -> CallbackHandlerKind.QuestExit
                                }
                                callbackHandlerWrites = callbackHandlerWrites +
                                        (CallbackHandlerSlot(kind) to setOf(label))
                            }
                            OP_CLR_QT_FAILURE.code,
                            OP_CLR_QT_SUCCESS.code,
                            OP_CLR_QT_CANCEL.code,
                            OP_CLR_QT_EXIT.code,
                            -> {
                                val kind = when (inst.opcode.code) {
                                    OP_CLR_QT_FAILURE.code -> CallbackHandlerKind.QuestFailure
                                    OP_CLR_QT_SUCCESS.code -> CallbackHandlerKind.QuestSuccess
                                    OP_CLR_QT_CANCEL.code -> CallbackHandlerKind.QuestCancel
                                    else -> CallbackHandlerKind.QuestExit
                                }
                                callbackHandlerWrites = callbackHandlerWrites +
                                        (CallbackHandlerSlot(kind) to emptySet())
                            }
                            OP_SET_QUEST_BOARD_HANDLER.code -> {
                                val index = (inst.args.getOrNull(0) as? IntArg)?.value ?: continue
                                if (index !in 0..5) continue
                                val label = (inst.args.getOrNull(1) as? IntArg)?.value ?: continue
                                val slot = CallbackHandlerSlot(CallbackHandlerKind.QuestBoard, index)
                                callbackHandlerWrites = callbackHandlerWrites + (slot to setOf(label))
                            }
                            OP_CLEAR_QUEST_BOARD_HANDLER.code -> {
                                val index = (inst.args.getOrNull(0) as? IntArg)?.value ?: continue
                                if (index !in 0..5) continue
                                val slot = CallbackHandlerSlot(CallbackHandlerKind.QuestBoard, index)
                                callbackHandlerWrites = callbackHandlerWrites + (slot to emptySet())
                            }
                        }
                        ordinaryThreadEdges[inst]?.forEach {
                            bfs(it, it.start, emptySet(), depth + 1, floorBound = false)
                                ?.let { exit ->
                                    commitFloorHandlerWrites(exit.floorHandlerWrites)
                                    commitCallbackHandlerWrites(exit.callbackHandlerWrites)
                                }
                        }
                        callbackEdges[inst]?.forEach {
                            bfs(it, it.start, emptySet(), depth + 1, floorBound = true)
                                ?.let { exit ->
                                    commitFloorHandlerWrites(exit.floorHandlerWrites)
                                    commitCallbackHandlerWrites(exit.callbackHandlerWrites)
                                }
                        }

                        // A normal QuestThread2 remains attached to the Quest object. At resume,
                        // analyze the continuation once for every possible runtime g_CurrentFloor.
                        // Floor-scoped callbacks/threads are destroyed by a floor transition, so
                        // their continuation remains bound to the same floor.
                        if (!floorBound && isResumableYield(inst)) {
                            commitFloorHandlerWrites(floorHandlerWrites)
                            commitCallbackHandlerWrites(callbackHandlerWrites)
                            if (i + 1 < b.end) {
                                enqueueResume(b, i + 1)
                            } else {
                                b.to.forEach { enqueueResume(it, it.start) }
                            }
                            yielded = true
                            break
                        }
                    }
                    if (yielded) {
                        suspended = true
                        break
                    }

                    // Walk the block (excluding terminator) to compute the post-block state
                    // even for Return blocks — pre-`ret` instructions like `let`/`leti` still
                    // contribute to the exit state propagated to the caller.
                    val outgoing = walkBlockTrackingFloorRegs(b, regs)
                    val terminator = if (b.end > b.start) b.segment.instructions[b.end - 1] else null

                    if (b.branchType == BranchType.Return) {
                        mergedExit = if (mergedExit == null) {
                            ThreadExitState(outgoing, floorHandlerWrites, callbackHandlerWrites)
                        } else {
                            ThreadExitState(
                                mergedExit.floorRegisters + outgoing,
                                mergeHandlerWrites(
                                    mergedExit.floorHandlerWrites,
                                    floorHandlerWrites,
                                ),
                                mergeHandlerWrites(
                                    mergedExit.callbackHandlerWrites,
                                    callbackHandlerWrites,
                                ),
                            )
                        }
                        continue
                    }

                    when (b.branchType) {
                        BranchType.Call -> {
                            val pruned = tryPruneSwitch(
                                terminator, outgoing, currentFloor, labelToEntryBlock,
                            )
                            val callees = pruned ?: b.to
                            // Recurse into each candidate callee, union their exit states.
                            // The after-call block inherits this union (not the pre-call state).
                            var mergedCalleeExit: ThreadExitState? = null
                            var anyCallee = false
                            var suspendedCallee = false
                            var fixedCalleeReturned = false
                            for (callee in callees) {
                                anyCallee = true
                                val calleeExit = bfs(
                                    callee,
                                    callee.start,
                                    outgoing,
                                    depth + 1,
                                    floorBound,
                                    floorHandlerWrites,
                                    callbackHandlerWrites,
                                )
                                if (calleeExit == null) {
                                    suspendedCallee = true
                                } else {
                                    fixedCalleeReturned = true
                                    mergedCalleeExit = if (mergedCalleeExit == null) {
                                        calleeExit
                                    } else {
                                        ThreadExitState(
                                            mergedCalleeExit.floorRegisters + calleeExit.floorRegisters,
                                            mergeHandlerWrites(
                                                mergedCalleeExit.floorHandlerWrites,
                                                calleeExit.floorHandlerWrites,
                                            ),
                                            mergeHandlerWrites(
                                                mergedCalleeExit.callbackHandlerWrites,
                                                calleeExit.callbackHandlerWrites,
                                            ),
                                        )
                                    }
                                }
                            }
                            nextInSegment[b]?.let {
                                if (suspendedCallee && !floorBound) enqueueResume(it, it.start)
                                if (!anyCallee || fixedCalleeReturned) {
                                    val afterCallState = if (anyCallee) {
                                        checkNotNull(mergedCalleeExit)
                                    } else {
                                        ThreadExitState(
                                            outgoing, floorHandlerWrites, callbackHandlerWrites,
                                        )
                                    }
                                    queue.add(
                                        TraversalState(
                                            it,
                                            it.start,
                                            afterCallState.floorRegisters,
                                            floorBound,
                                            afterCallState.floorHandlerWrites,
                                            afterCallState.callbackHandlerWrites,
                                        ),
                                    )
                                }
                            }
                        }
                        BranchType.Jump -> {
                            val pruned = tryPruneSwitch(
                                terminator, outgoing, currentFloor, labelToEntryBlock,
                            )
                            val succs = pruned ?: b.to
                            for (s in succs) {
                                queue.add(
                                    TraversalState(
                                        s,
                                        s.start,
                                        outgoing,
                                        floorBound,
                                        floorHandlerWrites,
                                        callbackHandlerWrites,
                                    ),
                                )
                            }
                        }
                        BranchType.ConditionalJump -> {
                            val pruned = tryPruneConditional(
                                b, terminator, outgoing, currentFloor,
                                labelToEntryBlock, nextInSegment,
                            )
                            val succs = pruned ?: b.to
                            for (s in succs) {
                                queue.add(
                                    TraversalState(
                                        s,
                                        s.start,
                                        outgoing,
                                        floorBound,
                                        floorHandlerWrites,
                                        callbackHandlerWrites,
                                    ),
                                )
                            }
                        }
                        BranchType.None -> {
                            for (s in b.to) {
                                queue.add(
                                    TraversalState(
                                        s,
                                        s.start,
                                        outgoing,
                                        floorBound,
                                        floorHandlerWrites,
                                        callbackHandlerWrites,
                                    ),
                                )
                            }
                        }
                        BranchType.Return -> { /* handled above */ }
                    }
                }

                inProgress.remove(key)
                if (suspended) return null
                val exit = mergedExit ?: ThreadExitState(
                    startRegs, startFloorHandlerWrites, startCallbackHandlerWrites,
                )
                exitStateCache[key] = exit
                return exit
            }

        bfs(entryBlock, entryInstruction, emptySet(), 0, entryFloorBound)
            ?.let {
                commitFloorHandlerWrites(it.floorHandlerWrites)
                commitCallbackHandlerWrites(it.callbackHandlerWrites)
            }
    }

    return ParticleExecutionFloors(result)
}

/** Opcodes after which the same client quest thread can resume on a later frame. */
private fun isResumableYield(inst: Instruction): Boolean = when (inst.opcode.mnemonic) {
    "sync",
    "list",
    "fadein",
    "fadeout",
    "window_msg",
    "disp_msg_qb",
    "add_msg",
    "message",
    "award_item_name",
    "award_item_select",
    "award_item_ok",
    "get_item_id",
    "chat_box",
    -> true
    else -> false
}

/**
 * Returns the offset of a script callback label embedded in an opcode's first register group.
 * These opcodes all create floor-local client objects. The offsets come from the PSO opcode
 * layouts: most use (x, y, z, radius, label), while party-coordinate triggers insert a second
 * radius before the label.
 */
private fun spatialCallbackLabelRegisterOffset(opcodeCode: Int): Int? = when (opcodeCode) {
    OP_AT_COORDS_CALL.code,
    OP_AT_COORDS_TALK.code,
    OP_COL_NPCIN.code,
    OP_SET_OBJ_PARAM.code,
    OP_COL_PLINAW.code,
    OP_SET_CHAT_CALLBACK.code,
    OP_SET_CHAT_CALLBACK_NO_FILTER.code,
    OP_AT_COORDS_CALL_EX.code,
    OP_AT_COORDS_TALK_EX.code,
    OP_NPC_COORDS_CALL_EX.code,
    OP_SET_OBJ_PARAM_EX.code,
    OP_NPC_CHECK_STRAGGLE_EX.code,
    -> 4

    OP_COL_NPCINR.code,
    OP_PARTY_COORDS_CALL_EX.code,
    -> 5

    else -> null
}

/**
 * Walks `block` instruction by instruction, updating which registers currently hold the BFS
 * instance's "current floor" value. Skips the terminator: the terminator is examined separately
 * for branch pruning and never writes to the floor-tracking lineage.
 */
private fun walkBlockTrackingFloorRegs(
    block: BasicBlock,
    incoming: Set<Int>,
): Set<Int> {
    var regs = incoming
    val end = if (block.branchType != BranchType.None) block.end - 1 else block.end
    for (i in block.start until end) {
        regs = updateFloorRegsForInstruction(block.segment.instructions[i], regs)
    }
    return regs
}

private fun updateFloorRegsForInstruction(
    inst: Instruction,
    regs: Set<Int>,
): Set<Int> {
    when (inst.opcode.code) {
        OP_GET_FLOOR_NUMBER.code -> {
            // get_floor_number slot, baseReg writes floor at +0 and room at +1.
            val baseReg = (inst.args.getOrNull(1) as? IntArg)?.value ?: return regs
            return (regs + baseReg) - (baseReg + 1)
        }
        OP_LET.code -> {
            // let dest, src copies src's value into dest.
            val dest = (inst.args.getOrNull(0) as? IntArg)?.value ?: return regs
            val src = (inst.args.getOrNull(1) as? IntArg)?.value ?: return regs
            return if (src in regs) regs + dest else regs - dest
        }
        OP_LETI.code -> {
            // A literal assignment breaks floor-value lineage. Even when the literal happens
            // to equal the current floor, it did not originate from get_floor_number and must not
            // be used to prune later branches. Quest scripts use small literals pervasively;
            // treating (for example) every `leti rX, 5` as the floor value on floor 5 causes
            // unrelated conditionals to be pruned and makes reachable particle sites disappear.
            val dest = (inst.args.getOrNull(0) as? IntArg)?.value ?: return regs
            return regs - dest
        }
        else -> {
            // Default: any opcode whose declared RegType params include a writable register
            // clears that register from the floor-tracking set.
            var result = regs
            val params = inst.opcode.params
            val argLen = minOf(inst.args.size, params.size)
            for (j in 0 until argLen) {
                val type = params[j].type
                if (type !is RegType) continue
                val regs2 = type.registers ?: continue
                val regRef = (inst.args[j] as? IntArg)?.value ?: continue
                for ((k, regParam) in regs2.withIndex()) {
                    if (regParam.write) result = result - (regRef + k)
                }
            }
            return result
        }
    }
}

/**
 * For [OP_SWITCH_JMP] / [OP_SWITCH_CALL] terminators that dispatch on a register currently
 * holding the BFS's current floor value, returns the single branch consistent with that floor
 * (an empty list if the floor is out of range). Returns `null` when no pruning is possible.
 */
private fun tryPruneSwitch(
    inst: Instruction?,
    floorRegs: Set<Int>,
    currentFloor: Int,
    labelToEntryBlock: Map<Int, BasicBlock>,
): List<BasicBlock>? {
    if (inst == null) return null
    if (inst.opcode.code != OP_SWITCH_JMP.code && inst.opcode.code != OP_SWITCH_CALL.code) {
        return null
    }
    val reg = (inst.args.getOrNull(0) as? IntArg)?.value ?: return null
    if (reg !in floorRegs) return null
    // switch_jmp/switch_call args: args[0]=reg, args[1+i]=label_i.
    // If currentFloor is out of range of the label table, the switch is a no-op at runtime
    // and execution falls through. Return null so the caller follows the default edges
    // (b.to + sequential next-in-segment) rather than dropping all successors.
    val labelArg = inst.args.getOrNull(currentFloor + 1) as? IntArg ?: return null
    val target = labelToEntryBlock[labelArg.value] ?: return null
    return listOf(target)
}

/**
 * Pruning for [BranchType.ConditionalJump] terminators. Handles [OP_SWITCH_JMP],
 * [OP_JMPI_E], [OP_JMPI_NE] when the discriminant is a floor-tracking register.
 * Returns `null` when no pruning is possible.
 */
private fun tryPruneConditional(
    b: BasicBlock,
    inst: Instruction?,
    floorRegs: Set<Int>,
    currentFloor: Int,
    labelToEntryBlock: Map<Int, BasicBlock>,
    nextInSegment: Map<BasicBlock, BasicBlock>,
): List<BasicBlock>? {
    if (inst == null) return null

    if (inst.opcode.code == OP_SWITCH_JMP.code) {
        return tryPruneSwitch(inst, floorRegs, currentFloor, labelToEntryBlock)
    }

    if (inst.opcode.code == OP_JMPI_E.code || inst.opcode.code == OP_JMPI_NE.code) {
        val reg = (inst.args.getOrNull(0) as? IntArg)?.value ?: return null
        if (reg !in floorRegs) return null
        val constVal = (inst.args.getOrNull(1) as? IntArg)?.value ?: return null
        val labelArg = inst.args.getOrNull(2) as? IntArg ?: return null
        val takeBranch = when (inst.opcode.code) {
            OP_JMPI_E.code -> currentFloor == constVal
            OP_JMPI_NE.code -> currentFloor != constVal
            else -> error("unreachable")
        }
        return if (takeBranch) {
            listOfNotNull(labelToEntryBlock[labelArg.value])
        } else {
            listOfNotNull(nextInSegment[b])
        }
    }

    return null
}
