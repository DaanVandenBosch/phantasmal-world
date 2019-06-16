import { observable, IObservableArray, computed } from "mobx";

const NORMAL_DAMAGE_FACTOR = 0.2 * 0.9;
const HEAVY_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 1.89;
const SAC_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 3.32;
const VJAYA_DAMAGE_FACTOR = NORMAL_DAMAGE_FACTOR * 5.56;
const CRIT_FACTOR = 1.5;

class WeaponType {
    constructor(
        readonly minAtp: number,
        readonly maxAtp: number
    ) { }
}

class Weapon {
    readonly type: WeaponType;

    /**
     * Integer from 0 to 100.
     */
    @observable attributePercentage: number = 0;
    @observable grind: number = 0;

    @computed get shiftaAtp(): number {
        if (this.type.minAtp === this.type.maxAtp) {
            return 0;
        } else {
            return this.type.maxAtp * this.store.shiftaFactor;
        }
    }

    @computed get grindAtp(): number {
        return 2 * this.grind;
    }

    @computed get minAtp(): number {
        return this.type.minAtp + this.grindAtp;
    }

    @computed get maxAtp(): number {
        return this.type.maxAtp + this.grindAtp + this.shiftaAtp;
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
        type: WeaponType,
    ) {
        this.type = type;
    }
}

class DpsCalcStore {
    //
    // Character Details
    //

    @observable charAtp: number = 0;
    @observable magPow: number = 0;
    @observable armorAtp: number = 0;
    @observable shieldAtp: number = 0;
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

    //
    // Enemy Details
    //

    @observable enemyDfp: number = 0;
}

export const dpsCalcStore = new DpsCalcStore();
