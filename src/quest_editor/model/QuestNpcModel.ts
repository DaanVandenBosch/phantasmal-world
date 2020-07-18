import { QuestEntityModel } from "./QuestEntityModel";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { Property } from "../../core/observable/property/Property";
import { defined } from "../../core/util";
import { property } from "../../core/observable";
import { WaveModel } from "./WaveModel";
import { QuestNpc } from "../../core/data_formats/parsing/quest/Quest";

export class QuestNpcModel extends QuestEntityModel<NpcType, QuestNpc> {
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
        this.entity.wave = wave_id;
        this.entity.wave_2 = wave_id;
        this._wave.val = wave;
        return this;
    }
}
