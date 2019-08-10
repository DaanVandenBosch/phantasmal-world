import Logger from "js-logger";
import { Instruction } from "../instructions";
import {
    Kind,
    MAX_SIGNED_DWORD_VALUE,
    MIN_SIGNED_DWORD_VALUE,
    Opcode,
    ParamAccess,
} from "../opcodes";
import { BasicBlock, ControlFlowGraph } from "./ControlFlowGraph";
import { ValueSet } from "./ValueSet";

const logger = Logger.get("scripting/data_flow_analysis/register_value");

export const MIN_REGISTER_VALUE = MIN_SIGNED_DWORD_VALUE;
export const MAX_REGISTER_VALUE = MAX_SIGNED_DWORD_VALUE;
export const REGISTER_VALUES = Math.pow(2, 32);

/**
 * Computes the possible values of a register right before a specific instruction.
 */
export function register_value(
    cfg: ControlFlowGraph,
    instruction: Instruction,
    register: number,
): ValueSet {
    const block = cfg.get_block_for_instruction(instruction);

    if (block) {
        return find_values(
            new Context(),
            new Set(),
            block,
            block.index_of_instruction(instruction),
            register,
        );
    } else {
        return new ValueSet();
    }
}

class Context {
    iterations = 0;
}

function find_values(
    ctx: Context,
    path: Set<BasicBlock>,
    block: BasicBlock,
    end: number,
    register: number,
): ValueSet {
    if (++ctx.iterations > 100) {
        logger.warn("Too many iterations.");
        return new ValueSet().set_interval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE);
    }

    for (let i = end - 1; i >= block.start; i--) {
        const instruction = block.segment.instructions[i];
        const args = instruction.args;

        switch (instruction.opcode) {
            case Opcode.LET:
                if (args[0].value === register) {
                    return find_values(ctx, new Set(path), block, i, args[1].value);
                }
                break;
            case Opcode.LETI:
            case Opcode.LETB:
            case Opcode.LETW:
            case Opcode.SYNC_LETI:
                if (args[0].value === register) {
                    return new ValueSet().set_value(args[1].value);
                }
                break;
            case Opcode.SET:
                if (args[0].value === register) {
                    return new ValueSet().set_value(1);
                }
                break;
            case Opcode.CLEAR:
                if (args[0].value === register) {
                    return new ValueSet().set_value(0);
                }
                break;
            case Opcode.REV:
                if (args[0].value === register) {
                    const prev_vals = find_values(ctx, new Set(path), block, i, register);
                    const prev_size = prev_vals.size();

                    if (prev_size === 0 || (prev_size === 1 && prev_vals.get(0) === 0)) {
                        return new ValueSet().set_value(1);
                    } else if (prev_vals.has(0)) {
                        return new ValueSet().set_interval(0, 1);
                    } else {
                        return new ValueSet().set_value(0);
                    }
                }
                break;
            case Opcode.ADDI:
                if (args[0].value === register) {
                    const prev_vals = find_values(ctx, new Set(path), block, i, register);
                    return prev_vals.scalar_add(args[1].value);
                }
                break;
            case Opcode.SUBI:
                if (args[0].value === register) {
                    const prev_vals = find_values(ctx, new Set(path), block, i, register);
                    return prev_vals.scalar_sub(args[1].value);
                }
                break;
            case Opcode.MULI:
                if (args[0].value === register) {
                    const prev_vals = find_values(ctx, new Set(path), block, i, register);
                    return prev_vals.scalar_mul(args[1].value);
                }
                break;
            case Opcode.DIVI:
                if (args[0].value === register) {
                    const prev_vals = find_values(ctx, new Set(path), block, i, register);
                    return prev_vals.scalar_div(args[1].value);
                }
                break;
            case Opcode.IF_ZONE_CLEAR:
                if (args[0].value === register) {
                    return new ValueSet().set_interval(0, 1);
                }
                break;
            case Opcode.GET_DIFFLVL:
                if (args[0].value === register) {
                    return new ValueSet().set_interval(0, 2);
                }
                break;
            case Opcode.GET_SLOTNUMBER:
                if (args[0].value === register) {
                    return new ValueSet().set_interval(0, 3);
                }
                break;
            case Opcode.GET_RANDOM:
                if (args[1].value === register) {
                    // TODO: undefined values.
                    const min = find_values(ctx, new Set(path), block, i, args[0].value).min() || 0;
                    const max = Math.max(
                        find_values(ctx, new Set(path), block, i, args[0].value + 1).max() || 0,
                        min + 1,
                    );
                    return new ValueSet().set_interval(min, max - 1);
                }
                break;
            case Opcode.STACK_PUSHM:
            case Opcode.STACK_POPM:
                {
                    const min_reg = args[0].value;
                    const max_reg = args[0].value + args[1].value;

                    if (min_reg <= register && register < max_reg) {
                        return new ValueSet().set_interval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE);
                    }
                }
                break;
            default:
                // Assume any other opcodes that write to the register can produce any value.
                {
                    const params = instruction.opcode.params;
                    const arg_len = Math.min(args.length, params.length);

                    for (let j = 0; j < arg_len; j++) {
                        const param = params[j];

                        if (param.type.kind == Kind.RegTupRef) {
                            const reg_ref = args[j].value;
                            let k = 0;

                            for (const reg_param of param.type.register_tuples) {
                                if (
                                    (reg_param.access === ParamAccess.Write ||
                                        reg_param.access === ParamAccess.ReadWrite) &&
                                    reg_ref + k === register
                                ) {
                                    return new ValueSet().set_interval(
                                        MIN_REGISTER_VALUE,
                                        MAX_REGISTER_VALUE,
                                    );
                                }

                                k++;
                            }
                        }
                    }
                }
                break;
        }
    }

    const values = new ValueSet();
    path.add(block);

    for (const from of block.from) {
        // Bail out from loops.
        if (path.has(from)) {
            values.set_interval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE);
            break;
        }

        values.union(find_values(ctx, new Set(path), from, from.end, register));
    }

    return values;
}
