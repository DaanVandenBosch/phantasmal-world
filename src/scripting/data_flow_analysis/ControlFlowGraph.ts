import { Instruction, InstructionSegment, Segment, SegmentType } from "../instructions";
import { Opcode } from "../opcodes";

export enum BranchType {
    None,
    Return,
    Jump,
    ConditionalJump,
    Call,
}

export class BasicBlock {
    readonly from: BasicBlock[] = [];
    readonly to: BasicBlock[] = [];

    constructor(
        readonly segment: InstructionSegment,
        readonly start: number,
        readonly end: number,
        readonly branch_type: BranchType,
        /**
         * Either jumps or calls, depending on `branch_type`.
         */
        readonly branch_labels: number[]
    ) {}

    link_to(other: BasicBlock): void {
        if (!this.to.includes(other)) {
            this.to.push(other);
            other.from.push(this);
        }
    }
}

export class ControlFlowGraph {
    readonly blocks: BasicBlock[] = [];

    private readonly instructions_to_block: Map<Instruction, BasicBlock> = new Map();
    private readonly labels_to_block = new Map<number, BasicBlock>();

    get_block_for_instuction(instruction: Instruction): BasicBlock | undefined {
        return this.instructions_to_block.get(instruction);
    }

    get_block_for_label(label: number): BasicBlock | undefined {
        return this.labels_to_block.get(label);
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

    private static create_basic_blocks(cfg: ControlFlowGraph, segment: InstructionSegment) {
        const len = segment.instructions.length;
        let start = 0;
        let first_block = true;

        for (let i = 0; i < len; i++) {
            const inst = segment.instructions[i];

            let branch_type: BranchType;
            let branch_labels: number[];

            switch (inst.opcode) {
                // Return.
                case Opcode.RET:
                    branch_type = BranchType.Return;
                    branch_labels = [];
                    break;

                // Unconditional jump.
                case Opcode.JMP:
                    branch_type = BranchType.Jump;
                    branch_labels = [inst.args[0].value];
                    break;

                // Conditional jumps.
                case Opcode.JMP_ON:
                case Opcode.JMP_OFF:
                    branch_type = BranchType.ConditionalJump;
                    branch_labels = [inst.args[0].value];
                    break;
                case Opcode.JMP_E:
                case Opcode.JMPI_E:
                case Opcode.JMP_NE:
                case Opcode.JMPI_NE:
                case Opcode.UJMP_G:
                case Opcode.UJMPI_G:
                case Opcode.JMP_G:
                case Opcode.JMPI_G:
                case Opcode.UJMP_L:
                case Opcode.UJMPI_L:
                case Opcode.JMP_L:
                case Opcode.JMPI_L:
                case Opcode.UJMP_GE:
                case Opcode.UJMPI_GE:
                case Opcode.JMP_GE:
                case Opcode.JMPI_GE:
                case Opcode.UJMP_LE:
                case Opcode.UJMPI_LE:
                case Opcode.JMP_LE:
                case Opcode.JMPI_LE:
                    branch_type = BranchType.ConditionalJump;
                    branch_labels = [inst.args[2].value];
                    break;
                case Opcode.SWITCH_JMP:
                    branch_type = BranchType.ConditionalJump;
                    branch_labels = inst.args.slice(1).map(a => a.value);
                    break;

                // Calls.
                case Opcode.CALL:
                    branch_type = BranchType.Call;
                    branch_labels = [inst.args[0].value];
                    break;
                case Opcode.VA_CALL:
                    branch_type = BranchType.Call;
                    branch_labels = [inst.args[0].value];
                    break;
                case Opcode.SWITCH_CALL:
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
 * @param ret Block the caller should return to.
 * @param caller Calling block.
 */
function link_returning_blocks(
    label_blocks: Map<number, BasicBlock>,
    ret: BasicBlock,
    caller: BasicBlock
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
 */
function link_returning_blocks_recurse(
    encountered: Set<BasicBlock>,
    ret: BasicBlock,
    block: BasicBlock
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
