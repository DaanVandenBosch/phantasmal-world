import { QuestEntityModel } from "./QuestEntityModel";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { Euler, Vector3 } from "three";

export class QuestNpcModel extends QuestEntityModel<NpcType> {
    readonly pso_type_id: number;
    readonly npc_id: number;
    readonly script_label: number;
    readonly pso_roaming: number;
    readonly scale: Vector3;
    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    readonly unknown: readonly number[][];

    constructor(
        type: NpcType,
        pso_type_id: number,
        npc_id: number,
        script_label: number,
        pso_roaming: number,
        area_id: number,
        section_id: number,
        position: Vector3,
        rotation: Euler,
        scale: Vector3,
        unknown: readonly number[][],
    ) {
        if (!Number.isInteger(pso_type_id)) throw new Error("pso_type_id should be an integer.");
        if (!Number.isFinite(npc_id)) throw new Error("npc_id should be a number.");
        if (!Number.isInteger(script_label)) throw new Error("script_label should be an integer.");
        if (!Number.isInteger(pso_roaming)) throw new Error("pso_roaming should be an integer.");
        if (!scale) throw new Error("scale is required.");
        if (!unknown) throw new Error("unknown is required.");
        if (unknown.length !== 3)
            throw new Error(`unknown should be of length 3, was ${unknown.length}.`);
        if (unknown[0].length !== 10)
            throw new Error(`unknown[0] should be of length 10, was ${unknown[0].length}`);
        if (unknown[1].length !== 6)
            throw new Error(`unknown[1] should be of length 6, was ${unknown[1].length}`);
        if (unknown[2].length !== 4)
            throw new Error(`unknown[2] should be of length 4, was ${unknown[2].length}`);

        super(type, area_id, section_id, position, rotation);

        this.pso_type_id = pso_type_id;
        this.npc_id = npc_id;
        this.script_label = script_label;
        this.pso_roaming = pso_roaming;
        this.unknown = unknown;
        this.scale = scale;
    }
}
