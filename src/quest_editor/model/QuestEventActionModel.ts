export abstract class QuestEventActionModel {}

export class QuestEventActionSpawnNpcsModel extends QuestEventActionModel {
    readonly section_id: number;
    readonly appear_flag: number;

    constructor(section_id: number, appear_flag: number) {
        super();

        this.section_id = section_id;
        this.appear_flag = appear_flag;
    }
}

export class QuestEventActionUnlockModel extends QuestEventActionModel {
    readonly door_id: number;

    constructor(door_id: number) {
        super();

        this.door_id = door_id;
    }
}

export class QuestEventActionLockModel extends QuestEventActionModel {
    readonly door_id: number;

    constructor(door_id: number) {
        super();

        this.door_id = door_id;
    }
}

export class QuestEventActionSpawnWaveModel extends QuestEventActionModel {
    readonly wave_id: number;

    constructor(wave_id: number) {
        super();

        this.wave_id = wave_id;
    }
}
