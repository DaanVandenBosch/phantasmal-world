import { QuestEntityModel } from "./QuestEntityModel";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { Euler, Vector3 } from "three";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { Property } from "../../core/observable/property/Property";
import {
    assert,
    require_finite,
    require_integer,
    require_non_negative_integer,
    defined,
} from "../../core/util";
import { property } from "../../core/observable";

export class QuestNpcModel extends QuestEntityModel<NpcType> {
    private readonly _wave: WritableProperty<number>;
    private _pso_wave2: number;

    readonly pso_type_id: number;
    readonly npc_id: number;
    readonly wave: Property<number>;

    get pso_wave2(): number {
        return this._pso_wave2;
    }

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
        wave: number,
        pso_wave2: number,
        script_label: number,
        pso_roaming: number,
        area_id: number,
        section_id: number,
        position: Vector3,
        rotation: Euler,
        scale: Vector3,
        unknown: readonly number[][],
    ) {
        require_integer(pso_type_id, "pso_type_id");
        require_finite(npc_id, "npc_id");
        require_non_negative_integer(wave, "wave");
        require_non_negative_integer(pso_wave2, "pso_wave2");
        require_integer(script_label, "script_label");
        require_integer(pso_roaming, "pso_roaming");
        defined(scale, "scale");
        defined(unknown, "unknown");
        assert(unknown.length === 2, () => `unknown should be of length 2, was ${unknown.length}.`);
        assert(
            unknown[0].length === 10,
            () => `unknown[0] should be of length 10, was ${unknown[0].length}`,
        );
        assert(
            unknown[1].length === 4,
            () => `unknown[1] should be of length 4, was ${unknown[1].length}`,
        );

        super(type, area_id, section_id, position, rotation);

        this.pso_type_id = pso_type_id;
        this.npc_id = npc_id;
        this._wave = property(wave);
        this.wave = this._wave;
        this._pso_wave2 = pso_wave2;
        this.script_label = script_label;
        this.pso_roaming = pso_roaming;
        this.unknown = unknown;
        this.scale = scale;
    }

    set_wave(wave: number): this {
        this._wave.val = wave;
        this._pso_wave2 = wave;
        return this;
    }
}
