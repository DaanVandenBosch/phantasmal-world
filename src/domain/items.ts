import { observable, computed } from "mobx";

//
// Item types.
// Instances of these classes contain the data that is the same for every item of a specific type.
// E.g. all spread needles are called "Spread Needle" and they all have the same ATA.
//

export interface ItemType {
    readonly id: number,
    readonly name: string
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
    ) { }
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
    ) { }
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
    ) { }
}

export class UnitItemType implements ItemType {
    constructor(
        readonly id: number,
        readonly name: string,
    ) { }
}

export class ToolItemType implements ItemType {
    constructor(
        readonly id: number,
        readonly name: string,
    ) { }
}

//
// Item instances.
// Instances of these classes contain the data that is unique to each item.
// E.g. a specific spread needle dropped by an enemy or in an inventory.
//

export interface Item {
    readonly type: ItemType,
}

export class WeaponItem implements Item {
    /**
     * Integer from 0 to 100.
     */
    @observable attribute: number = 0;
    /**
     * Integer from 0 to 100.
     */
    @observable hit: number = 0;
    @observable grind: number = 0;

    @computed get grind_atp(): number {
        return 2 * this.grind;
    }

    constructor(
        readonly type: WeaponItemType,
    ) { }
}

export class ArmorItem implements Item {
    constructor(
        readonly type: ArmorItemType,
    ) { }
}

export class ShieldItem implements Item {
    constructor(
        readonly type: ShieldItemType,
    ) { }
}

export class UnitItem implements Item {
    constructor(
        readonly type: UnitItemType,
    ) { }
}

export class ToolItem implements Item {
    constructor(
        readonly type: ToolItemType,
    ) { }
}