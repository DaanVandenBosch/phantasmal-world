import { observable, IObservableArray, computed } from "mobx";
import { WeaponItem, WeaponItemType, ArmorItemType, ShieldItemType } from "../domain";
import { itemTypeStores } from "./ItemTypeStore";

const NORMAL_DAMAGE_FACTOR = 0.2 * 0.9;
const HEAVY_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 1.89;
// const SAC_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 3.32;
// const VJAYA_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 5.56;
// const CRIT_FACTOR = 1.5;

class Weapon {
    readonly item: WeaponItem;

    @computed get shiftaAtp(): number {
        if (this.item.type.minAtp === this.item.type.maxAtp) {
            return 0;
        } else {
            return this.item.type.maxAtp * this.store.shiftaFactor;
        }
    }

    @computed get minAtp(): number {
        return this.item.type.minAtp + this.item.grindAtp;
    }

    @computed get maxAtp(): number {
        return this.item.type.maxAtp + this.item.grindAtp + this.shiftaAtp;
    }

    @computed get finalMinAtp(): number {
        return this.minAtp
            + this.store.armorAtp
            + this.store.shieldAtp
            + this.store.baseAtp
            + this.store.baseShiftaAtp;
    }

    @computed get finalMaxAtp(): number {
        return this.maxAtp
            + this.store.armorAtp
            + this.store.shieldAtp
            + this.store.baseAtp
            + this.store.baseShiftaAtp;
    }

    @computed get minNormalDamage(): number {
        return (this.finalMinAtp - this.store.enemyDfp) * NORMAL_DAMAGE_FACTOR;
    }

    @computed get maxNormalDamage(): number {
        return (this.finalMaxAtp - this.store.enemyDfp) * NORMAL_DAMAGE_FACTOR;
    }

    @computed get avgNormalDamage(): number {
        return (this.minNormalDamage + this.maxNormalDamage) / 2;
    }

    @computed get minHeavyDamage(): number {
        return (this.finalMinAtp - this.store.enemyDfp) * HEAVY_DAMAGE_FACTOR;
    }

    @computed get maxHeavyDamage(): number {
        return (this.finalMaxAtp - this.store.enemyDfp) * HEAVY_DAMAGE_FACTOR;
    }

    @computed get avgHeavyDamage(): number {
        return (this.minHeavyDamage + this.maxHeavyDamage) / 2;
    }

    constructor(
        private store: DpsCalcStore,
        item: WeaponItem,
    ) {
        this.item = item;
    }
}

class DpsCalcStore {
    @computed get weaponTypes(): WeaponItemType[] {
        return itemTypeStores.current.value.itemTypes.filter(it =>
            it instanceof WeaponItemType
        ) as WeaponItemType[];
    }

    @computed get armorTypes(): ArmorItemType[] {
        return itemTypeStores.current.value.itemTypes.filter(it =>
            it instanceof ArmorItemType
        ) as ArmorItemType[];
    }

    @computed get shieldTypes(): ShieldItemType[] {
        return itemTypeStores.current.value.itemTypes.filter(it =>
            it instanceof ShieldItemType
        ) as ShieldItemType[];
    }

    //
    // Character Details
    //

    @observable charAtp: number = 0;
    @observable magPow: number = 0;
    @computed get armorAtp(): number { return this.armorType ? this.armorType.atp : 0 }
    @computed get shieldAtp(): number { return this.shieldType ? this.shieldType.atp : 0 }
    @observable shiftaLvl: number = 0;

    @computed get baseAtp(): number {
        return this.charAtp + 2 * this.magPow;
    }

    @computed get shiftaFactor(): number {
        return this.shiftaLvl ? 0.013 * (this.shiftaLvl - 1) + 0.1 : 0;
    }

    @computed get baseShiftaAtp(): number {
        return this.baseAtp * this.shiftaFactor;
    }

    @observable readonly weapons: IObservableArray<Weapon> = observable.array();

    addWeapon = (type: WeaponItemType) => {
        this.weapons.push(new Weapon(
            this,
            new WeaponItem(type)
        ));
    }

    @observable armorType?: ArmorItemType;
    @observable shieldType?: ShieldItemType;

    //
    // Enemy Details
    //

    @observable enemyDfp: number = 0;
}

export const dpsCalcStore = new DpsCalcStore();
