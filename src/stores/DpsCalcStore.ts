import { observable, IObservableArray, computed } from "mobx";
import { WeaponItem, WeaponItemType, ArmorItemType, ShieldItemType } from "../domain";
import { item_type_stores } from "./ItemTypeStore";

const NORMAL_DAMAGE_FACTOR = 0.2 * 0.9;
const HEAVY_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 1.89;
// const SAC_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 3.32;
// const VJAYA_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 5.56;
// const CRIT_FACTOR = 1.5;

class Weapon {
    private readonly store: DpsCalcStore;
    readonly item: WeaponItem;

    @computed get shifta_atp(): number {
        if (this.item.type.min_atp === this.item.type.max_atp) {
            return 0;
        } else {
            return this.item.type.max_atp * this.store.shifta_factor;
        }
    }

    @computed get min_atp(): number {
        return this.item.type.min_atp + this.item.grind_atp;
    }

    @computed get max_atp(): number {
        return this.item.type.max_atp + this.item.grind_atp + this.shifta_atp;
    }

    @computed get final_min_atp(): number {
        return (
            this.min_atp +
            this.store.armor_atp +
            this.store.shield_atp +
            this.store.base_atp +
            this.store.base_shifta_atp
        );
    }

    @computed get final_max_atp(): number {
        return (
            this.max_atp +
            this.store.armor_atp +
            this.store.shield_atp +
            this.store.base_atp +
            this.store.base_shifta_atp
        );
    }

    @computed get min_normal_damage(): number {
        return (this.final_min_atp - this.store.enemy_dfp) * NORMAL_DAMAGE_FACTOR;
    }

    @computed get max_normal_damage(): number {
        return (this.final_max_atp - this.store.enemy_dfp) * NORMAL_DAMAGE_FACTOR;
    }

    @computed get avg_normal_damage(): number {
        return (this.min_normal_damage + this.max_normal_damage) / 2;
    }

    @computed get min_heavy_damage(): number {
        return (this.final_min_atp - this.store.enemy_dfp) * HEAVY_DAMAGE_FACTOR;
    }

    @computed get max_heavy_damage(): number {
        return (this.final_max_atp - this.store.enemy_dfp) * HEAVY_DAMAGE_FACTOR;
    }

    @computed get avg_heavy_damage(): number {
        return (this.min_heavy_damage + this.max_heavy_damage) / 2;
    }

    constructor(store: DpsCalcStore, item: WeaponItem) {
        this.store = store;
        this.item = item;
    }
}

class DpsCalcStore {
    @computed get weapon_types(): WeaponItemType[] {
        return item_type_stores.current.value.item_types.filter(
            it => it instanceof WeaponItemType
        ) as WeaponItemType[];
    }

    @computed get armor_types(): ArmorItemType[] {
        return item_type_stores.current.value.item_types.filter(
            it => it instanceof ArmorItemType
        ) as ArmorItemType[];
    }

    @computed get shield_types(): ShieldItemType[] {
        return item_type_stores.current.value.item_types.filter(
            it => it instanceof ShieldItemType
        ) as ShieldItemType[];
    }

    //
    // Character Details
    //

    @observable char_atp: number = 0;
    @observable mag_pow: number = 0;
    @computed get armor_atp(): number {
        return this.armor_type ? this.armor_type.atp : 0;
    }
    @computed get shield_atp(): number {
        return this.shield_type ? this.shield_type.atp : 0;
    }
    @observable shifta_lvl: number = 0;

    @computed get base_atp(): number {
        return this.char_atp + 2 * this.mag_pow;
    }

    @computed get shifta_factor(): number {
        return this.shifta_lvl ? 0.013 * (this.shifta_lvl - 1) + 0.1 : 0;
    }

    @computed get base_shifta_atp(): number {
        return this.base_atp * this.shifta_factor;
    }

    @observable readonly weapons: IObservableArray<Weapon> = observable.array();

    add_weapon = (type: WeaponItemType) => {
        this.weapons.push(new Weapon(this, new WeaponItem(type)));
    };

    @observable armor_type?: ArmorItemType;
    @observable shield_type?: ShieldItemType;

    //
    // Enemy Details
    //

    @observable enemy_dfp: number = 0;
}

export const dps_calc_store = new DpsCalcStore();
