import { QuestEntityModel } from "./QuestEntityModel";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { Property } from "../../core/observable/property/Property";
import { defined } from "../../core/util";
import { property } from "../../core/observable";
import { WaveModel } from "./WaveModel";
import {
    get_npc_position,
    get_npc_rotation,
    get_npc_section_id,
    get_npc_type,
    QuestNpc,
    set_npc_position,
    set_npc_rotation,
    set_npc_section_id,
    set_npc_wave,
    set_npc_wave_2,
} from "../../core/data_formats/parsing/quest/QuestNpc";
import { Vec3 } from "../../core/data_formats/vector";

export class QuestNpcModel extends QuestEntityModel<NpcType, QuestNpc> {
    get type(): NpcType {
        return get_npc_type(this.entity);
    }

    private readonly _wave: WritableProperty<WaveModel | undefined>;

    readonly wave: Property<WaveModel | undefined>;

    constructor(npc: QuestNpc, wave?: WaveModel) {
        defined(npc, "npc");

        super(npc);

        this._wave = property(wave);
        this.wave = this._wave;
    }

    set_wave(wave?: WaveModel): this {
        const wave_id = wave?.id?.val ?? 0;
        set_npc_wave(this.entity, wave_id);
        set_npc_wave_2(this.entity, wave_id);
        this._wave.val = wave;
        return this;
    }

    protected get_entity_section_id(): number {
        return get_npc_section_id(this.entity);
    }

    protected set_entity_section_id(section_id: number): void {
        set_npc_section_id(this.entity, section_id);
    }

    protected get_entity_position(): Vec3 {
        return get_npc_position(this.entity);
    }

    protected set_entity_position(position: Vec3): void {
        set_npc_position(this.entity, position);
    }

    protected get_entity_rotation(): Vec3 {
        return get_npc_rotation(this.entity);
    }

    protected set_entity_rotation(rotation: Vec3): void {
        set_npc_rotation(this.entity, rotation);
    }
}
