import { InputNumber } from "antd";
import { observer } from "mobx-react";
import React, { ReactNode, Component } from "react";
import { WeaponItemType, ArmorItemType, ShieldItemType } from "../../domain";
import { dps_calc_store } from "../../stores/DpsCalcStore";
import { item_type_stores } from "../../stores/ItemTypeStore";
import { BigSelect } from "../BigSelect";

@observer
export class DpsCalcComponent extends Component {
    render(): ReactNode {
        return (
            <section>
                <section>
                    <div>Weapons:</div>
                    <BigSelect
                        placeholder="Add a weapon"
                        value={undefined}
                        options={dps_calc_store.weapon_types.map(wt => ({
                            label: wt.name,
                            value: wt.id,
                        }))}
                        onChange={this.add_weapon}
                    />
                    <table>
                        <thead>
                            <tr>
                                <td>Weapon</td>
                                <td>Min. ATP</td>
                                <td>Max. ATP</td>
                                <td>Grind</td>
                                <td>Grind ATP</td>
                                <td>Shifta ATP</td>
                                <td>Final Min. ATP</td>
                                <td>Final Max. ATP</td>
                                <td>Min. Normal Damage</td>
                                <td>Max. Normal Damage</td>
                                <td>Avg. Normal Damage</td>
                                <td>Min. Heavy Damage</td>
                                <td>Max. Heavy Damage</td>
                                <td>Avg. Heavy Damage</td>
                            </tr>
                        </thead>
                        <tbody>
                            {dps_calc_store.weapons.map((weapon, i) => (
                                <tr key={i}>
                                    <td>{weapon.item.type.name}</td>
                                    <td>{weapon.item.type.min_atp}</td>
                                    <td>{weapon.item.type.max_atp}</td>
                                    <td>
                                        <InputNumber
                                            size="small"
                                            value={weapon.item.grind}
                                            min={0}
                                            max={weapon.item.type.max_grind}
                                            step={1}
                                            onChange={value => (weapon.item.grind = value || 0)}
                                        />
                                    </td>
                                    <td>{weapon.item.grind_atp}</td>
                                    <td>{weapon.shifta_atp.toFixed(1)}</td>
                                    <td>{weapon.final_min_atp.toFixed(1)}</td>
                                    <td>{weapon.final_max_atp.toFixed(1)}</td>
                                    <td>{weapon.min_normal_damage.toFixed(1)}</td>
                                    <td>{weapon.max_normal_damage.toFixed(1)}</td>
                                    <td>{weapon.avg_normal_damage.toFixed(1)}</td>
                                    <td>{weapon.min_heavy_damage.toFixed(1)}</td>
                                    <td>{weapon.max_heavy_damage.toFixed(1)}</td>
                                    <td>{weapon.avg_heavy_damage.toFixed(1)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div>Character ATP:</div>
                    <InputNumber
                        value={dps_calc_store.char_atp}
                        min={0}
                        step={1}
                        onChange={value => (dps_calc_store.char_atp = value || 0)}
                    />
                    <div>MAG POW:</div>
                    <InputNumber
                        value={dps_calc_store.mag_pow}
                        min={0}
                        max={200}
                        step={1}
                        onChange={value => (dps_calc_store.mag_pow = value || 0)}
                    />
                    <div>Armor:</div>
                    <BigSelect
                        placeholder="Choose an armor"
                        value={dps_calc_store.armor_type && dps_calc_store.armor_type.id}
                        options={dps_calc_store.armor_types.map(at => ({
                            label: at.name,
                            value: at.id,
                        }))}
                        onChange={this.armor_changed}
                    />
                    <span>Armor ATP: {dps_calc_store.armor_atp}</span>
                    <div>Shield:</div>
                    <BigSelect
                        placeholder="Choose a shield"
                        value={dps_calc_store.shield_type && dps_calc_store.shield_type.id}
                        options={dps_calc_store.shield_types.map(st => ({
                            label: st.name,
                            value: st.id,
                        }))}
                        onChange={this.shield_changed}
                    />
                    <span>Shield ATP: {dps_calc_store.shield_atp}</span>
                    <div>Shifta level:</div>
                    <InputNumber
                        value={dps_calc_store.shifta_lvl}
                        min={0}
                        max={30}
                        step={1}
                        onChange={value => (dps_calc_store.shifta_lvl = value || 0)}
                    />
                    <div>Shifta factor:</div>
                    <div>{dps_calc_store.shifta_factor.toFixed(3)}</div>
                    <div>Base shifta ATP:</div>
                    <div>{dps_calc_store.base_shifta_atp.toFixed(2)}</div>
                </section>
            </section>
        );
    }

    private add_weapon = (selected: any) => {
        if (selected) {
            let type = item_type_stores.current.value.get_by_id(selected.value)!;
            dps_calc_store.add_weapon(type as WeaponItemType);
        }
    };

    private armor_changed = (selected: any) => {
        if (selected) {
            let item_type = item_type_stores.current.value.get_by_id(selected.value)!;
            dps_calc_store.armor_type = item_type as ArmorItemType;
        } else {
            dps_calc_store.armor_type = undefined;
        }
    };

    private shield_changed = (selected: any) => {
        if (selected) {
            let item_type = item_type_stores.current.value.get_by_id(selected.value)!;
            dps_calc_store.shield_type = item_type as ShieldItemType;
        } else {
            dps_calc_store.shield_type = undefined;
        }
    };
}
