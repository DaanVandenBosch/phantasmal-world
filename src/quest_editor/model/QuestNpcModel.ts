import { QuestEntityModel } from "./QuestEntityModel";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { Vec3 } from "../../core/data_formats/vector";

export class QuestNpcModel extends QuestEntityModel<NpcType> {
    readonly pso_type_id: number;
    readonly npc_id: number;
    readonly script_label: number;
    readonly roaming: number;
    readonly scale: Vec3;
    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    readonly unknown: number[][];

    constructor(
        type: NpcType,
        pso_type_id: number,
        npc_id: number,
        script_label: number,
        roaming: number,
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        scale: Vec3,
        unknown: number[][],
    ) {
        super(type, area_id, section_id, position, rotation);

        this.pso_type_id = pso_type_id;
        this.npc_id = npc_id;
        this.script_label = script_label;
        this.roaming = roaming;
        this.unknown = unknown;
        this.scale = scale;
    }
}
