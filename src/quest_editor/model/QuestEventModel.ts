import { QuestEventActionModel } from "./QuestEventActionModel";

export class QuestEventModel {
    readonly id: number;
    readonly section_id: number;
    readonly wave: number;
    readonly delay: number;
    readonly actions: readonly QuestEventActionModel[];
    readonly area_id: number;
    readonly unknown: number;

    constructor(
        id: number,
        section_id: number,
        wave: number,
        delay: number,
        actions: readonly QuestEventActionModel[],
        area_id: number,
        unknown: number,
    ) {
        if (!Number.isInteger(id)) throw new Error("id should be an integer.");
        if (!Number.isInteger(section_id)) throw new Error("section_id should be an integer.");
        if (!Number.isInteger(wave)) throw new Error("wave should be an integer.");
        if (!Number.isInteger(delay)) throw new Error("delay should be an integer.");
        if (!Array.isArray(actions)) throw new Error("actions should be an array.");
        if (!Number.isInteger(area_id)) throw new Error("area_id should be an integer.");
        if (!Number.isInteger(unknown)) throw new Error("unknown should be an integer.");

        this.id = id;
        this.section_id = section_id;
        this.wave = wave;
        this.delay = delay;
        this.actions = actions;
        this.area_id = area_id;
        this.unknown = unknown;
    }
}
