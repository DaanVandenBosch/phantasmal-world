import { Instruction, InstructionSegment } from "../instructions";
import {
    OP_CALL,
    OP_JMP,
    OP_JMP_E,
    OP_JMP_G,
    OP_JMP_GE,
    OP_JMP_L,
    OP_JMP_LE,
    OP_JMP_NE,
    OP_JMP_OFF,
    OP_JMP_ON,
    OP_JMPI_E,
    OP_JMPI_G,
    OP_JMPI_GE,
    OP_JMPI_L,
    OP_JMPI_LE,
    OP_JMPI_NE,
    OP_RET,
    OP_SWITCH_CALL,
    OP_SWITCH_JMP,
    OP_UJMP_G,
    OP_UJMP_GE,
    OP_UJMP_L,
    OP_UJMP_LE,
    OP_UJMPI_G,
    OP_UJMPI_GE,
    OP_UJMPI_L,
    OP_UJMPI_LE,
    OP_VA_CALL,
} from "../opcodes";

export enum BranchType {
    None,
    Return,
    Jump,
    ConditionalJump,
    Call,
}

export class BasicBlock {
    readonly segment: InstructionSegment;
    readonly start: number;
    readonly end: number;
    readonly branch_type: BranchType;
    /**
     * Either jumps or calls, depending on `branch_type`.
     */
    readonly branch_labels: number[];
    readonly from: BasicBlock[] = [];
    readonly to: BasicBlock[] = [];

    constructor(
        segment: InstructionSegment,
        start: number,
        end: number,
        branch_type: BranchType,
        branch_labels: number[],
    ) {
        this.segment = segment;
        this.start = start;
        this.end = end;
        this.branch_type = branch_type;
        this.branch_labels = branch_labels;
    }

    link_to(other: BasicBlock): void {
        if (!this.to.includes(other)) {
            this.to.push(other);
            other.from.push(this);
        }
    }

    index_of_instruction(instruction: Instruction): number {
        const index = this.segment.instructions.indexOf(instruction, this.start);
        return index < this.end ? index : -1;
    }
}

export class ControlFlowGraph {
    readonly blocks: BasicBlock[] = [];

    private readonly instructions_to_block: Map<Instruction, BasicBlock> = new Map();
    private readonly labels_to_block = new Map<number, BasicBlock>();

    get_block_for_instruction(instruction: Instruction): BasicBlock | undefined {
        return this.instructions_to_block.get(instruction);
    }

    static create(segments: InstructionSegment[]): ControlFlowGraph {
        const cfg = new ControlFlowGraph();
        // Mapping of labels to basic blocks.

        for (const segment of segments) {
            this.create_basic_blocks(cfg, segment);
        }

        this.link_blocks(cfg);
        return cfg;
    }

    private static create_basic_blocks(cfg: ControlFlowGraph, segment: InstructionSegment): void {
        const len = segment.instructions.length;
        let start = 0;
        let first_block = true;

        for (let i = 0; i < len; i++) {
            const inst = segment.instructions[i];

            let branch_type: BranchType;
            let branch_labels: number[];

            switch (inst.opcode.code) {
                // Return.
                case OP_RET.code:
                    branch_type = BranchType.Return;
                    branch_labels = [];
                    break;

                // Unconditional jump.
                case OP_JMP.code:
                    branch_type = BranchType.Jump;
                    branch_labels = [inst.args[0].value];
                    break;

                // Conditional jumps.
                case OP_JMP_ON.code:
                case OP_JMP_OFF.code:
                    branch_type = BranchType.ConditionalJump;
                    branch_labels = [inst.args[0].value];
                    break;
                case OP_JMP_E.code:
                case OP_JMPI_E.code:
                case OP_JMP_NE.code:
                case OP_JMPI_NE.code:
                case OP_UJMP_G.code:
                case OP_UJMPI_G.code:
                case OP_JMP_G.code:
                case OP_JMPI_G.code:
                case OP_UJMP_L.code:
                case OP_UJMPI_L.code:
                case OP_JMP_L.code:
                case OP_JMPI_L.code:
                case OP_UJMP_GE.code:
                case OP_UJMPI_GE.code:
                case OP_JMP_GE.code:
                case OP_JMPI_GE.code:
                case OP_UJMP_LE.code:
                case OP_UJMPI_LE.code:
                case OP_JMP_LE.code:
                case OP_JMPI_LE.code:
                    branch_type = BranchType.ConditionalJump;
                    branch_labels = [inst.args[2].value];
                    break;
                case OP_SWITCH_JMP.code:
                    branch_type = BranchType.ConditionalJump;
                    branch_labels = inst.args.slice(1).map(a => a.value);
                    break;

                // Calls.
                case OP_CALL.code:
                    branch_type = BranchType.Call;
                    branch_labels = [inst.args[0].value];
                    break;
                case OP_VA_CALL.code:
                    branch_type = BranchType.Call;
                    branch_labels = [inst.args[0].value];
                    break;
                case OP_SWITCH_CALL.code:
                    branch_type = BranchType.Call;
                    branch_labels = inst.args.slice(1).map(a => a.value);
                    break;

                // All other opcodes.
                default:
                    if (i === len - 1) {
                        branch_type = BranchType.None;
                        branch_labels = [];
                        break;
                    } else {
                        continue;
                    }
            }

            const block = new BasicBlock(segment, start, i + 1, branch_type, branch_labels);

            for (let j = block.start; j < block.end; j++) {
                cfg.instructions_to_block.set(block.segment.instructions[j], block);
            }

            cfg.blocks.push(block);

            if (first_block) {
                for (const label of segment.labels) {
                    cfg.labels_to_block.set(label, block);
                }

                first_block = false;
            }

            start = i + 1;
        }
    }

    private static link_blocks(cfg: ControlFlowGraph): void {
        // Pairs of calling block and block to which callees should return to.
        const callers: [BasicBlock, BasicBlock][] = [];

        for (let i = 0; i < cfg.blocks.length; i++) {
            const block = cfg.blocks[i];
            const next_block = cfg.blocks[i + 1];

            switch (block.branch_type) {
                case BranchType.Return:
                    continue;
                case BranchType.Call:
                    if (next_block) {
                        callers.push([block, next_block]);
                    }
                    break;
                case BranchType.None:
                case BranchType.ConditionalJump:
                    if (next_block) {
                        block.link_to(next_block);
                    }
                    break;
            }

            for (const label of block.branch_labels) {
                const to_block = cfg.labels_to_block.get(label);

                if (to_block) {
                    block.link_to(to_block);
                }
            }
        }

        for (const [caller, ret] of callers) {
            link_returning_blocks(cfg.labels_to_block, ret, caller);
        }
    }
}

/**
 * Links returning blocks to their callers.
 *
 * @param label_blocks
 * @param ret Block the caller should return to.
 * @param caller Calling block.
 */
function link_returning_blocks(
    label_blocks: Map<number, BasicBlock>,
    ret: BasicBlock,
    caller: BasicBlock,
): void {
    for (const label of caller.branch_labels) {
        const callee = label_blocks.get(label);

        if (callee) {
            if (callee.branch_type === BranchType.Return) {
                callee.link_to(ret);
            } else {
                link_returning_blocks_recurse(new Set(), ret, callee);
            }
        }
    }
}

/**
 * @param encountered For avoiding infinite loops.
 * @param ret
 * @param block
 */
function link_returning_blocks_recurse(
    encountered: Set<BasicBlock>,
    ret: BasicBlock,
    block: BasicBlock,
): void {
    if (encountered.has(block)) {
        return;
    } else {
        encountered.add(block);
    }

    for (const to_block of block.to) {
        if (to_block.branch_type === BranchType.Return) {
            to_block.link_to(ret);
        } else {
            link_returning_blocks_recurse(encountered, ret, to_block);
        }
    }
}
