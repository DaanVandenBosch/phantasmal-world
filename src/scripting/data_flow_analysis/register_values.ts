import { Instruction, Opcode } from "../../data_formats/parsing/quest/bin";
import { BasicBlock, ControlFlowGraph } from "./ControlFlowGraph";
import { ValueSet } from "./ValueSet";

const MIN_REGISTER_VALUE = -Math.pow(2, 31);
const MAX_REGISTER_VALUE = Math.pow(2, 31) - 1;

/**
 * Computes the possible values of a register at a specific instruction.
 */
export function register_values(
    cfg: ControlFlowGraph,
    instruction: Instruction,
    register: number
): ValueSet {
    const block = cfg.instructions.get(instruction);

    if (block) {
        let inst_idx = block.start;

        while (inst_idx < block.end) {
            if (block.segment.instructions[inst_idx] === instruction) {
                break;
            }

            inst_idx++;
        }

        return find_values(block, inst_idx, register);
    } else {
        return new ValueSet();
    }
}

function find_values(block: BasicBlock, end: number, register: number): ValueSet {
    let values = new ValueSet();

    for (let i = block.start; i < end; i++) {
        const instruction = block.segment.instructions[i];
        const args = instruction.args;

        switch (instruction.opcode) {
            case Opcode.let:
                if (args[0].value === register) {
                    values = find_values(block, i, args[1].value);
                }
                break;
            case Opcode.leti:
            case Opcode.letb:
            case Opcode.letw:
                if (args[0].value === register) {
                    values.set_value(args[1].value);
                }
                break;
            case Opcode.leta:
            case Opcode.leto:
                if (args[0].value === register) {
                    values.set_interval(MIN_REGISTER_VALUE, MAX_REGISTER_VALUE);
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
                    const prev_vals = find_values(block, i, register);
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
                    values = find_values(block, i, register);
                    values.scalar_add(args[1].value);
                }
                break;
            case Opcode.subi:
                if (args[0].value === register) {
                    values = find_values(block, i, register);
                    values.scalar_sub(args[1].value);
                }
                break;
            case Opcode.muli:
                if (args[0].value === register) {
                    values = find_values(block, i, register);
                    values.scalar_mul(args[1].value);
                }
                break;
            case Opcode.divi:
                if (args[0].value === register) {
                    values = find_values(block, i, register);
                    values.scalar_div(args[1].value);
                }
                break;
            case Opcode.get_random:
                if (args[1].value === register) {
                    // TODO: undefined values.
                    const min = find_values(block, i, args[0].value).min() || 0;
                    const max = Math.max(
                        find_values(block, i, args[0].value + 1).max() || 0,
                        min + 1
                    );
                    values.set_interval(min, max - 1);
                }
                break;
        }
    }

    if (values.size() === 0) {
        for (const from of block.from) {
            values.union(find_values(from, from.end, register));
        }
    }

    return values;
}
