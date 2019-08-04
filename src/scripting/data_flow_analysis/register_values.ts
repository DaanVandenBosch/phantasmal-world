import {
    Instruction,
    Opcode,
    RegTupRefType,
    TYPE_REG_REF,
    TYPE_REG_REF_VAR,
} from "../../data_formats/parsing/quest/bin";
import { BasicBlock, ControlFlowGraph } from "./ControlFlowGraph";
import { ValueSet } from "./ValueSet";

export const MIN_REGISTER_VALUE = -Math.pow(2, 31);
export const MAX_REGISTER_VALUE = Math.pow(2, 31) - 1;
export const REGISTER_VALUES = Math.pow(2, 32);

/**
 * Computes the possible values of a register at a specific instruction.
 */
export function register_values(
    cfg: ControlFlowGraph,
    instruction: Instruction,
    register: number
): ValueSet {
    const block = cfg.get_block_for_instuction(instruction);

    if (block) {
        let inst_idx = block.start;

        while (inst_idx < block.end) {
            if (block.segment.instructions[inst_idx] === instruction) {
                break;
            }

            inst_idx++;
        }

        return find_values(new Set(), block, inst_idx, register);
    } else {
        return new ValueSet();
    }
}

function find_values(
    path: Set<BasicBlock>,
    block: BasicBlock,
    end: number,
    register: number
): ValueSet {
    let values = new ValueSet();

    for (let i = block.start; i < end; i++) {
        const instruction = block.segment.instructions[i];
        const args = instruction.args;

        switch (instruction.opcode) {
            case Opcode.let:
                if (args[0].value === register) {
                    values = find_values(new Set(path), block, i, args[1].value);
                }
                break;
            case Opcode.leti:
            case Opcode.letb:
            case Opcode.letw:
            case Opcode.sync_leti:
                if (args[0].value === register) {
                    values.set_value(args[1].value);
                }
                break;
            case Opcode.set:
                if (args[0].value === register) {
                    values.set_value(1);
                }
                break;
            case Opcode.clear:
                if (args[0].value === register) {
                    values.set_value(0);
                }
                break;
            case Opcode.rev:
                if (args[0].value === register) {
                    const prev_vals = find_values(new Set(path), block, i, register);
                    const prev_size = prev_vals.size();

                    if (prev_size === 0 || (prev_size === 1 && prev_vals.get(0) === 0)) {
                        values.set_value(1);
                    } else if (values.has(0)) {
                        values.set_interval(0, 1);
                    } else {
                        values.set_value(0);
                    }
                }
                break;
            case Opcode.addi:
                if (args[0].value === register) {
                    values = find_values(new Set(path), block, i, register);
                    values.scalar_add(args[1].value);
                }
                break;
            case Opcode.subi:
                if (args[0].value === register) {
                    values = find_values(new Set(path), block, i, register);
                    values.scalar_sub(args[1].value);
                }
                break;
            case Opcode.muli:
                if (args[0].value === register) {
                    values = find_values(new Set(path), block, i, register);
                    values.scalar_mul(args[1].value);
                }
                break;
            case Opcode.divi:
                if (args[0].value === register) {
                    values = find_values(new Set(path), block, i, register);
                    values.scalar_div(args[1].value);
                }
                break;
            case Opcode.if_zone_clear:
                if (args[0].value === register) {
                    values.set_interval(0, 1);
                }
                break;
            case Opcode.get_difflvl:
            case Opcode.get_slotnumber:
                if (args[0].value === register) {
                    values.set_interval(0, 3);
                }
                break;
            case Opcode.get_random:
                if (args[1].value === register) {
                    // TODO: undefined values.
                    const min = find_values(new Set(path), block, i, args[0].value).min() || 0;
                    const max = Math.max(
                        find_values(new Set(path), block, i, args[0].value + 1).max() || 0,
                        min + 1
                    );
                    values.set_interval(min, max - 1);
                }
                break;
            default:
                // Assume any other opcodes that write to the register can produce any value.
                {
                    const params = instruction.opcode.params;
                    const len = Math.min(args.length, params.length);

                    for (let j = 0; j < len; j++) {
                        const param = params[j];
                        const val = args[j].value;

                        if (param.write) {
                            if (
                                (param.type instanceof RegTupRefType &&
                                    register >= val &&
                                    register < val + param.type.registers.length) ||
                                (param.type === TYPE_REG_REF && val.includes(register)) ||
                                (param.type === TYPE_REG_REF_VAR && val.includes(register))
                            ) {
                                values.set_interval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE);
                                break;
                            }
                        }
                    }
                }
                break;
        }
    }

    if (values.size() === 0) {
        path.add(block);

        for (const from of block.from) {
            // Bail out from loops.
            if (path.has(from)) {
                values.set_interval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE);
                break;
            }

            values.union(find_values(new Set(path), from, from.end, register));
        }
    }

    return values;
}
