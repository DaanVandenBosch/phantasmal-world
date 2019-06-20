import { InputNumber } from "antd";
import { observer } from "mobx-react";
import React from "react";
import { WeaponItemType, ArmorItemType, ShieldItemType } from "../../domain";
import { dpsCalcStore } from "../../stores/DpsCalcStore";
import { itemTypeStores } from "../../stores/ItemTypeStore";
import { BigSelect } from "../BigSelect";

@observer
export class DpsCalcComponent extends React.Component {
    render() {
        return (
            <section>
                <section>
                    <div>Weapons:</div>
                    <BigSelect
                        placeholder="Add a weapon"
                        value={undefined}
                        options={dpsCalcStore.weaponTypes.map(wt => ({
                            label: wt.name,
                            value: wt.id
                        }))}
                        onChange={this.addWeapon}
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
                            {dpsCalcStore.weapons.map((weapon, i) => (
                                <tr key={i}>
                                    <td>{weapon.item.type.name}</td>
                                    <td>{weapon.item.type.minAtp}</td>
                                    <td>{weapon.item.type.maxAtp}</td>
                                    <td>
                                        <InputNumber
                                            size="small"
                                            value={weapon.item.grind}
                                            min={0}
                                            max={weapon.item.type.maxGrind}
                                            step={1}
                                            onChange={(value) => weapon.item.grind = value || 0}
                                        />
                                    </td>
                                    <td>{weapon.item.grindAtp}</td>
                                    <td>{weapon.shiftaAtp.toFixed(1)}</td>
                                    <td>{weapon.finalMinAtp.toFixed(1)}</td>
                                    <td>{weapon.finalMaxAtp.toFixed(1)}</td>
                                    <td>{weapon.minNormalDamage.toFixed(1)}</td>
                                    <td>{weapon.maxNormalDamage.toFixed(1)}</td>
                                    <td>{weapon.avgNormalDamage.toFixed(1)}</td>
                                    <td>{weapon.minHeavyDamage.toFixed(1)}</td>
                                    <td>{weapon.maxHeavyDamage.toFixed(1)}</td>
                                    <td>{weapon.avgHeavyDamage.toFixed(1)}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    <div>Character ATP:</div>
                    <InputNumber
                        value={dpsCalcStore.charAtp}
                        min={0}
                        step={1}
                        onChange={(value) => dpsCalcStore.charAtp = value || 0}
                    />
                    <div>MAG POW:</div>
                    <InputNumber
                        value={dpsCalcStore.magPow}
                        min={0}
                        max={200}
                        step={1}
                        onChange={(value) => dpsCalcStore.magPow = value || 0}
                    />
                    <div>Armor:</div>
                    <BigSelect
                        placeholder="Choose an armor"
                        value={dpsCalcStore.armorType && dpsCalcStore.armorType.id}
                        options={dpsCalcStore.armorTypes.map(at => ({
                            label: at.name,
                            value: at.id
                        }))}
                        onChange={this.armorChanged}
                    />
                    <span>Armor ATP: {dpsCalcStore.armorAtp}</span>
                    <div>Shield:</div>
                    <BigSelect
                        placeholder="Choose a shield"
                        value={dpsCalcStore.shieldType && dpsCalcStore.shieldType.id}
                        options={dpsCalcStore.shieldTypes.map(st => ({
                            label: st.name,
                            value: st.id
                        }))}
                        onChange={this.shieldChanged}
                    />
                    <span>Shield ATP: {dpsCalcStore.shieldAtp}</span>
                    <div>Shifta level:</div>
                    <InputNumber
                        value={dpsCalcStore.shiftaLvl}
                        min={0}
                        max={30}
                        step={1}
                        onChange={(value) => dpsCalcStore.shiftaLvl = value || 0}
                    />
                    <div>Shifta factor:</div>
                    <div>{dpsCalcStore.shiftaFactor.toFixed(3)}</div>
                    <div>Base shifta ATP:</div>
                    <div>{dpsCalcStore.baseShiftaAtp.toFixed(2)}</div>
                </section>
            </section>
        );
    }

    private addWeapon = (selected: any) => {
        if (selected) {
            let type = itemTypeStores.current.value.getById(selected.value)!;
            dpsCalcStore.addWeapon(type as WeaponItemType);
        }
    }

    private armorChanged = (selected: any) => {
        if (selected) {
            let type = itemTypeStores.current.value.getById(selected.value)!;
            dpsCalcStore.armorType = (type as ArmorItemType);
        } else {
            dpsCalcStore.armorType = undefined;
        }
    }

    private shieldChanged = (selected: any) => {
        if (selected) {
            let type = itemTypeStores.current.value.getById(selected.value)!;
            dpsCalcStore.shieldType = (type as ShieldItemType);
        } else {
            dpsCalcStore.shieldType = undefined;
        }
    }
}
