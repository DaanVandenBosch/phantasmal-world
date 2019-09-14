import { WeaponItem, WeaponItemType, ArmorItemType, ShieldItemType } from "../../core/model/items";
import { item_type_stores, ItemTypeStore } from "../../core/stores/ItemTypeStore";
import { Property } from "../../core/observable/property/Property";
import { list_property, map, property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { ListProperty } from "../../core/observable/property/list/ListProperty";
import { WritableListProperty } from "../../core/observable/property/list/WritableListProperty";
import { sequential } from "../../core/sequential";
import { Disposable } from "../../core/observable/Disposable";

const NORMAL_DAMAGE_FACTOR = 0.2 * 0.9;
const HEAVY_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 1.89;
// const SAC_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 3.32;
// const VJAYA_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 5.56;
// const CRIT_FACTOR = 1.5;

class Weapon {
    readonly shifta_atp: Property<number> = this.store.shifta_factor.map(shifta_factor => {
        if (this.item.type.min_atp === this.item.type.max_atp) {
            return 0;
        } else {
            return this.item.type.max_atp * shifta_factor;
        }
    });

    readonly min_atp: Property<number> = this.item.grind_atp.map(
        grind_atp => this.item.type.min_atp + grind_atp,
    );

    readonly max_atp: Property<number> = map(
        (grind_atp, shifta_atp) => this.item.type.max_atp + grind_atp + shifta_atp,
        this.item.grind_atp,
        this.shifta_atp,
    );

    readonly final_min_atp: Property<number> = map(
        (min_atp, armor_atp, shield_atp, base_atp, base_shifta_atp) =>
            min_atp + armor_atp + shield_atp + base_atp + base_shifta_atp,
        this.min_atp,
        this.store.armor_atp,
        this.store.shield_atp,
        this.store.base_atp,
        this.store.base_shifta_atp,
    );

    readonly final_max_atp: Property<number> = map(
        (max_atp, armor_atp, shield_atp, base_atp, base_shifta_atp) =>
            max_atp + armor_atp + shield_atp + base_atp + base_shifta_atp,
        this.max_atp,
        this.store.armor_atp,
        this.store.shield_atp,
        this.store.base_atp,
        this.store.base_shifta_atp,
    );

    readonly min_normal_damage: Property<number> = map(
        (final_min_atp, enemy_dfp) => (final_min_atp - enemy_dfp) * NORMAL_DAMAGE_FACTOR,
        this.final_min_atp,
        this.store.enemy_dfp,
    );

    readonly max_normal_damage: Property<number> = map(
        (final_max_atp, enemy_dfp) => (final_max_atp - enemy_dfp) * NORMAL_DAMAGE_FACTOR,
        this.final_max_atp,
        this.store.enemy_dfp,
    );

    readonly avg_normal_damage: Property<number> = map(
        (min_normal_damage, max_normal_damage) => (min_normal_damage + max_normal_damage) / 2,
        this.min_normal_damage,
        this.max_normal_damage,
    );

    readonly min_heavy_damage: Property<number> = map(
        (final_min_atp, enemy_dfp) => (final_min_atp - enemy_dfp) * HEAVY_DAMAGE_FACTOR,
        this.final_min_atp,
        this.store.enemy_dfp,
    );

    readonly max_heavy_damage: Property<number> = map(
        (final_max_atp, enemy_dfp) => (final_max_atp - enemy_dfp) * HEAVY_DAMAGE_FACTOR,
        this.final_max_atp,
        this.store.enemy_dfp,
    );

    readonly avg_heavy_damage: Property<number> = map(
        (min_heavy_damage, max_heavy_damage) => (min_heavy_damage + max_heavy_damage) / 2,
        this.min_heavy_damage,
        this.max_heavy_damage,
    );

    constructor(private readonly store: DpsCalcStore, readonly item: WeaponItem) {}
}

class DpsCalcStore implements Disposable {
    private readonly _weapon_types: WritableListProperty<WeaponItemType> = list_property();
    private readonly _armor_types: WritableListProperty<ArmorItemType> = list_property();
    private readonly _shield_types: WritableListProperty<ShieldItemType> = list_property();
    private readonly _char_atp = property(0);
    private readonly _mag_pow = property(0);
    private readonly _shifta_lvl = property(0);
    private readonly _weapons: WritableListProperty<Weapon> = list_property();
    private readonly _armor_type: WritableProperty<ArmorItemType | undefined> = property(undefined);
    private readonly _shield_type: WritableProperty<ShieldItemType | undefined> = property(
        undefined,
    );
    private readonly _enemy_dfp = property(0);
    private readonly disposable: Disposable;

    //
    // Public Properties
    //

    readonly weapon_types: ListProperty<WeaponItemType> = this._weapon_types;
    readonly armor_types: ListProperty<ArmorItemType> = this._armor_types;
    readonly shield_types: ListProperty<ShieldItemType> = this._shield_types;

    //
    // Character Details
    //

    readonly char_atp: Property<number> = this._char_atp;
    readonly mag_pow: Property<number> = this._mag_pow;

    readonly armor_atp: Property<number> = this._armor_type.map(armor_type =>
        armor_type ? armor_type.atp : 0,
    );

    readonly shield_atp: Property<number> = this._shield_type.map(shield_type =>
        shield_type ? shield_type.atp : 0,
    );

    readonly shifta_lvl: Property<number> = this._shifta_lvl;
    readonly base_atp: Property<number> = map(
        (char_atp, mag_pow) => char_atp + 2 * mag_pow,
        this.char_atp,
        this.mag_pow,
    );
    readonly shifta_factor: Property<number> = this.shifta_lvl.map(shifta_lvl =>
        shifta_lvl ? 0.013 * (shifta_lvl - 1) + 0.1 : 0,
    );
    readonly base_shifta_atp: Property<number> = map(
        (base_atp, shifta_factor) => base_atp * shifta_factor,
        this.base_atp,
        this.shifta_factor,
    );
    readonly weapons: ListProperty<Weapon> = this._weapons;
    readonly armor_type: Property<ArmorItemType | undefined> = this._armor_type;
    readonly shield_type: Property<ShieldItemType | undefined> = this._shield_type;

    //
    // Enemy Details
    //

    readonly enemy_dfp: Property<number> = this._enemy_dfp;

    constructor() {
        this.disposable = item_type_stores.current.observe(
            sequential(async ({ value: item_type_store }: { value: Promise<ItemTypeStore> }) => {
                const weapon_types: WeaponItemType[] = [];
                const armor_types: ArmorItemType[] = [];
                const shield_types: ShieldItemType[] = [];

                for (const item_type of (await item_type_store).item_types) {
                    if (item_type instanceof WeaponItemType) {
                        weapon_types.push(item_type);
                    } else if (item_type instanceof ArmorItemType) {
                        armor_types.push(item_type);
                    } else if (item_type instanceof ShieldItemType) {
                        shield_types.push(item_type);
                    }
                }

                this._weapon_types.val = weapon_types;
                this._armor_types.val = armor_types;
                this._shield_types.val = shield_types;
            }),
        );
    }

    dispose = (): void => {
        this.disposable.dispose();
    };

    add_weapon = (type: WeaponItemType) => {
        this._weapons.push(new Weapon(this, new WeaponItem(type)));
    };
}

export const dps_calc_store = new DpsCalcStore();
