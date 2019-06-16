import { InputNumber } from "antd";
import { observer } from "mobx-react";
import React from "react";
import { dpsCalcStore } from "../../stores/DpsCalcStore";

@observer
export class DpsCalcComponent extends React.Component {
    render() {
        return (
            <section>
                <section>
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
                    <div>Armor ATP:</div>
                    <InputNumber
                        value={dpsCalcStore.armorAtp}
                        min={0}
                        step={1}
                        onChange={(value) => dpsCalcStore.armorAtp = value || 0}
                    />
                    <div>Shield ATP:</div>
                    <InputNumber
                        value={dpsCalcStore.shieldAtp}
                        min={0}
                        step={1}
                        onChange={(value) => dpsCalcStore.shieldAtp = value || 0}
                    />
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
}
