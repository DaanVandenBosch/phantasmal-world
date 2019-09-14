import Logger from "js-logger";
import { Instruction } from "../instructions";
import {
    MAX_SIGNED_DWORD_VALUE,
    MIN_SIGNED_DWORD_VALUE,
    Opcode,
    StackInteraction,
} from "../opcodes";
import { BasicBlock, ControlFlowGraph } from "./ControlFlowGraph";
import { ValueSet } from "./ValueSet";
import { register_value } from "./register_value";

const logger = Logger.get("quest_editor/scripting/data_flow_analysis/stack_value");

export const MIN_STACK_VALUE = MIN_SIGNED_DWORD_VALUE;
export const MAX_STACK_VALUE = MAX_SIGNED_DWORD_VALUE;

/**
 * Computes the possible values of a stack element at the nth position from the top right before a specific instruction.
 */
export function stack_value(
    cfg: ControlFlowGraph,
    instruction: Instruction,
    position: number,
): ValueSet {
    const block = cfg.get_block_for_instruction(instruction);

    if (block) {
        return find_values(
            new Context(cfg),
            new Set(),
            block,
            block.index_of_instruction(instruction),
            position,
        );
    } else {
        return new ValueSet();
    }
}

class Context {
    iterations = 0;

    constructor(readonly cfg: ControlFlowGraph) {}
}

function find_values(
    ctx: Context,
    path: Set<BasicBlock>,
    block: BasicBlock,
    end: number,
    position: number,
): ValueSet {
    if (++ctx.iterations > 100) {
        logger.warn("Too many iterations.");
        return new ValueSet().set_interval(MIN_STACK_VALUE, MAX_STACK_VALUE);
    }

    for (let i = end - 1; i >= block.start; i--) {
        const instruction = block.segment.instructions[i];

        if (instruction.opcode.stack === StackInteraction.Pop) {
            position += instruction.opcode.params.length;
            continue;
        }

        const args = instruction.args;

        switch (instruction.opcode) {
            case Opcode.ARG_PUSHR:
                if (position === 0) {
                    return register_value(ctx.cfg, instruction, args[0].value);
                } else {
                    position--;
                    break;
                }
            case Opcode.ARG_PUSHL:
            case Opcode.ARG_PUSHB:
            case Opcode.ARG_PUSHW:
                if (position === 0) {
                    return new ValueSet().set_value(args[0].value);
                } else {
                    position--;
                    break;
                }
            case Opcode.ARG_PUSHA:
            case Opcode.ARG_PUSHO:
            case Opcode.ARG_PUSHS:
                if (position === 0) {
                    return new ValueSet().set_interval(MIN_STACK_VALUE, MAX_STACK_VALUE);
                } else {
                    position--;
                    break;
                }
            default:
                break;
        }
    }

    const values = new ValueSet();
    path.add(block);

    for (const from of block.from) {
        // Bail out from loops.
        if (path.has(from)) {
            values.set_interval(MIN_STACK_VALUE, MAX_STACK_VALUE);
            break;
        }

        values.union(find_values(ctx, new Set(path), from, from.end, position));
    }

    return values;
}
