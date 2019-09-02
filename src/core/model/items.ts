import { Property } from "../observable/property/Property";
import { WritableProperty } from "../observable/property/WritableProperty";
import { property } from "../observable";

//
// Item types.
// Instances of these classes contain the data that is the same for every item of a specific type.
// E.g. all spread needles are called "Spread Needle" and they all have the same ATA.
//

export interface ItemType {
    readonly id: number;
    readonly name: string;
}

export class WeaponItemType implements ItemType {
    constructor(
        readonly id: number,
        readonly name: string,
        readonly min_atp: number,
        readonly max_atp: number,
        readonly ata: number,
        readonly max_grind: number,
        readonly required_atp: number,
    ) {}
}

export class ArmorItemType implements ItemType {
    constructor(
        readonly id: number,
        readonly name: string,
        readonly atp: number,
        readonly ata: number,
        readonly min_evp: number,
        readonly max_evp: number,
        readonly min_dfp: number,
        readonly max_dfp: number,
        readonly mst: number,
        readonly hp: number,
        readonly lck: number,
    ) {}
}

export class ShieldItemType implements ItemType {
    constructor(
        readonly id: number,
        readonly name: string,
        readonly atp: number,
        readonly ata: number,
        readonly min_evp: number,
        readonly max_evp: number,
        readonly min_dfp: number,
        readonly max_dfp: number,
        readonly mst: number,
        readonly hp: number,
        readonly lck: number,
    ) {}
}

export class UnitItemType implements ItemType {
    constructor(readonly id: number, readonly name: string) {}
}

export class ToolItemType implements ItemType {
    constructor(readonly id: number, readonly name: string) {}
}

//
// Item instances.
// Instances of these classes contain the data that is unique to each item.
// E.g. a specific spread needle dropped by an enemy or in an inventory.
//

export interface Item {
    readonly type: ItemType;
}

export class WeaponItem implements Item {
    readonly type: WeaponItemType;

    /**
     * Integer from 0 to 100.
     */
    readonly attribute: Property<number>;

    /**
     * Integer from 0 to 100.
     */
    readonly hit: Property<number>;

    readonly grind: Property<number>;

    readonly grind_atp: Property<number>;

    private readonly _attribute: WritableProperty<number>;
    private readonly _hit: WritableProperty<number>;
    private readonly _grind: WritableProperty<number>;

    constructor(type: WeaponItemType) {
        this.type = type;

        this._attribute = property(0);
        this.attribute = this._attribute;

        this._hit = property(0);
        this.hit = this._hit;

        this._grind = property(0);
        this.grind = this._grind;

        this.grind_atp = this.grind.map(grind => 2 * grind);
    }
}

export class ArmorItem implements Item {
    constructor(readonly type: ArmorItemType) {}
}

export class ShieldItem implements Item {
    constructor(readonly type: ShieldItemType) {}
}

export class UnitItem implements Item {
    constructor(readonly type: UnitItemType) {}
}

export class ToolItem implements Item {
    constructor(readonly type: ToolItemType) {}
}
