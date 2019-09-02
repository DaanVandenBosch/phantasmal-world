import { ItemType } from "../../core/model/items";
import { DifficultyModel, SectionIdModel } from "../../core/model";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { Duration } from "luxon";

export class WantedItemModel {
    constructor(readonly item_type: ItemType, readonly amount: number) {}
}

export class OptimalResultModel {
    constructor(
        readonly wanted_items: ItemType[],
        readonly optimal_methods: OptimalMethodModel[],
    ) {}
}

export class OptimalMethodModel {
    constructor(
        readonly difficulty: DifficultyModel,
        readonly section_ids: SectionIdModel[],
        readonly method_name: string,
        readonly method_episode: Episode,
        readonly method_time: Duration,
        readonly runs: number,
        readonly item_counts: Map<ItemType, number>,
    ) {}
}
