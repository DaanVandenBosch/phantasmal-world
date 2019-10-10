export abstract class QuestWaveActionModel {}

export class QuestWaveActionSpawnNpcsModel extends QuestWaveActionModel {
    readonly section_id: number;
    readonly appear_flag: number;

    constructor(section_id: number, appear_flag: number) {
        super();

        this.section_id = section_id;
        this.appear_flag = appear_flag;
    }
}

export class QuestWaveActionUnlockModel extends QuestWaveActionModel {
    readonly door_id: number;

    constructor(door_id: number) {
        super();

        this.door_id = door_id;
    }
}

export class QuestWaveActionLockModel extends QuestWaveActionModel {
    readonly door_id: number;

    constructor(door_id: number) {
        super();

        this.door_id = door_id;
    }
}

export class QuestWaveActionSpawnWaveModel extends QuestWaveActionModel {
    readonly wave_id: number;

    constructor(wave_id: number) {
        super();

        this.wave_id = wave_id;
    }
}
