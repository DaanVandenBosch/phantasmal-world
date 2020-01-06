import { InstructionSegment } from "../instructions";
import { ControlFlowGraph } from "./ControlFlowGraph";
import { OP_BB_MAP_DESIGNATE, OP_MAP_DESIGNATE, OP_MAP_DESIGNATE_EX } from "../opcodes";
import { get_register_value } from "./get_register_value";
import { LogManager } from "../../../Logger";

const logger = LogManager.get("core/data_formats/asm/data_flow_analysis/map_designations");

export function get_map_designations(
    instruction_segments: InstructionSegment[],
    func_0_segment: InstructionSegment,
): Map<number, number> {
    const map_designations = new Map<number, number>();
    let cfg: ControlFlowGraph | undefined;

    for (const inst of func_0_segment.instructions) {
        switch (inst.opcode.code) {
            case OP_MAP_DESIGNATE.code:
            case OP_MAP_DESIGNATE_EX.code:
                {
                    if (!cfg) {
                        cfg = ControlFlowGraph.create(instruction_segments);
                    }

                    const area_id = get_register_value(cfg, inst, inst.args[0].value);

                    if (area_id.size() !== 1) {
                        logger.warning(`Couldn't determine area ID for map_designate instruction.`);
                        continue;
                    }

                    const variant_id_register =
                        inst.args[0].value + (inst.opcode.code === OP_MAP_DESIGNATE.code ? 2 : 3);
                    const variant_id = get_register_value(cfg, inst, variant_id_register);

                    if (variant_id.size() !== 1) {
                        logger.warning(
                            `Couldn't determine area variant ID for map_designate instruction.`,
                        );
                        continue;
                    }

                    map_designations.set(area_id.get(0)!, variant_id.get(0)!);
                }
                break;

            case OP_BB_MAP_DESIGNATE.code:
                map_designations.set(inst.args[0].value, inst.args[2].value);
                break;
        }
    }

    return map_designations;
}
