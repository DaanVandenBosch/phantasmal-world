import { ItemType } from "../../core/model/items";
import { Difficulty, SectionId } from "../../core/model";
import { Episode } from "../../core/data_formats/parsing/quest/Episode";
import { Duration } from "luxon";
import { Property } from "../../core/observable/property/Property";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { property } from "../../core/observable";

export class WantedItemModel {
    readonly item_type: ItemType;
    readonly amount: Property<number>;

    private readonly _amount: WritableProperty<number>;

    constructor(item_type: ItemType, amount: number) {
        this.item_type = item_type;
        this._amount = property(amount);
        this.amount = this._amount;
    }

    set_amount(amount: number): this {
        this._amount.val = amount;
        return this;
    }
}

export class OptimalResultModel {
    constructor(
        readonly wanted_items: ItemType[],
        readonly optimal_methods: OptimalMethodModel[],
    ) {}
}

export class OptimalMethodModel {
    constructor(
        readonly difficulty: Difficulty,
        readonly section_ids: SectionId[],
        readonly method_name: string,
        readonly method_episode: Episode,
        readonly method_time: Duration,
        readonly runs: number,
        readonly item_counts: Map<ItemType, number>,
    ) {}
}
