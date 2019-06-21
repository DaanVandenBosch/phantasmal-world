import { computed, observable } from 'mobx';
import { Object3D } from 'three';
import { ArrayBufferCursor } from '../bin-data/ArrayBufferCursor';
import { DatNpc, DatObject, DatUnknown } from '../bin-data/parsing/quest/dat';
import { NpcType } from './NpcType';
import { ObjectType } from './ObjectType';
import { enumValues } from '../enums';
import { ItemType } from './items';

export * from './items';
export * from './NpcType';
export * from './ObjectType';

export const RARE_ENEMY_PROB = 1 / 512;
export const KONDRIEU_PROB = 1 / 10;

export enum Server {
    Ephinea = 'Ephinea'
}

export const Servers: Server[] = enumValues(Server);

export enum Episode {
    I = 1,
    II = 2,
    IV = 4
}

export const Episodes: Episode[] = enumValues(Episode);

export function checkEpisode(episode: Episode) {
    if (!Episode[episode]) {
        throw new Error(`Invalid episode ${episode}.`);
    }
}

export enum SectionId {
    Viridia,
    Greenill,
    Skyly,
    Bluefull,
    Purplenum,
    Pinkal,
    Redria,
    Oran,
    Yellowboze,
    Whitill,
}

export const SectionIds: SectionId[] = enumValues(SectionId);

export enum Difficulty {
    Normal, Hard, VHard, Ultimate
}

export const Difficulties: Difficulty[] = enumValues(Difficulty);

export class Vec3 {
    x: number;
    y: number;
    z: number;

    constructor(x?: number, y?: number, z?: number) {
        this.x = x || 0;
        this.y = y || 0;
        this.z = z || 0;
    }

    add(v: Vec3): Vec3 {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    clone(x?: number, y?: number, z?: number) {
        return new Vec3(
            typeof x === 'number' ? x : this.x,
            typeof y === 'number' ? y : this.y,
            typeof z === 'number' ? z : this.z);
    }
};

export class Section {
    id: number;
    @observable position: Vec3;
    @observable yAxisRotation: number;

    @computed get sinYAxisRotation(): number {
        return Math.sin(this.yAxisRotation);
    }

    @computed get cosYAxisRotation(): number {
        return Math.cos(this.yAxisRotation);
    }

    constructor(
        id: number,
        position: Vec3,
        yAxisRotation: number
    ) {
        if (!Number.isInteger(id) || id < -1)
            throw new Error(`Expected id to be an integer greater than or equal to -1, got ${id}.`);
        if (!position) throw new Error('position is required.');
        if (typeof yAxisRotation !== 'number') throw new Error('yAxisRotation is required.');

        this.id = id;
        this.position = position;
        this.yAxisRotation = yAxisRotation;
    }
}

export class Quest {
    @observable name: string;
    @observable shortDescription: string;
    @observable longDescription: string;
    @observable questNo?: number;
    @observable episode: Episode;
    @observable areaVariants: AreaVariant[];
    @observable objects: QuestObject[];
    @observable npcs: QuestNpc[];
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    datUnkowns: DatUnknown[];
    /**
     * (Partial) raw BIN data that can't be parsed yet by Phantasmal.
     */
    binData: ArrayBufferCursor;

    constructor(
        name: string,
        shortDescription: string,
        longDescription: string,
        questNo: number | undefined,
        episode: Episode,
        areaVariants: AreaVariant[],
        objects: QuestObject[],
        npcs: QuestNpc[],
        datUnknowns: DatUnknown[],
        binData: ArrayBufferCursor
    ) {
        if (questNo != null && (!Number.isInteger(questNo) || questNo < 0)) throw new Error('questNo should be null or a non-negative integer.');
        checkEpisode(episode);
        if (!objects || !(objects instanceof Array)) throw new Error('objs is required.');
        if (!npcs || !(npcs instanceof Array)) throw new Error('npcs is required.');

        this.name = name;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        this.questNo = questNo;
        this.episode = episode;
        this.areaVariants = areaVariants;
        this.objects = objects;
        this.npcs = npcs;
        this.datUnkowns = datUnknowns;
        this.binData = binData;
    }
}

/**
 * Abstract class from which QuestNpc and QuestObject derive.
 */
export class QuestEntity {
    @observable areaId: number;

    private _sectionId: number;

    @computed get sectionId(): number {
        return this.section ? this.section.id : this._sectionId;
    }

    @observable section?: Section;

    /**
     * World position
     */
    @observable position: Vec3;

    @observable rotation: Vec3;

    /**
     * Section-relative position
     */
    @computed get sectionPosition(): Vec3 {
        let { x, y, z } = this.position;

        if (this.section) {
            const relX = x - this.section.position.x;
            const relY = y - this.section.position.y;
            const relZ = z - this.section.position.z;
            const sin = -this.section.sinYAxisRotation;
            const cos = this.section.cosYAxisRotation;
            const rotX = cos * relX + sin * relZ;
            const rotZ = -sin * relX + cos * relZ;
            x = rotX;
            y = relY;
            z = rotZ;
        }

        return new Vec3(x, y, z);
    }

    set sectionPosition(sectPos: Vec3) {
        let { x: relX, y: relY, z: relZ } = sectPos;

        if (this.section) {
            const sin = -this.section.sinYAxisRotation;
            const cos = this.section.cosYAxisRotation;
            const rotX = cos * relX - sin * relZ;
            const rotZ = sin * relX + cos * relZ;
            const x = rotX + this.section.position.x;
            const y = relY + this.section.position.y;
            const z = rotZ + this.section.position.z;
            this.position = new Vec3(x, y, z);
        }
    }

    object3d?: Object3D;

    constructor(
        areaId: number,
        sectionId: number,
        position: Vec3,
        rotation: Vec3
    ) {
        if (Object.getPrototypeOf(this) === Object.getPrototypeOf(QuestEntity))
            throw new Error('Abstract class should not be instantiated directly.');
        if (!Number.isInteger(areaId) || areaId < 0)
            throw new Error(`Expected areaId to be a non-negative integer, got ${areaId}.`);
        if (!Number.isInteger(sectionId) || sectionId < 0)
            throw new Error(`Expected sectionId to be a non-negative integer, got ${sectionId}.`);
        if (!position) throw new Error('position is required.');
        if (!rotation) throw new Error('rotation is required.');

        this.areaId = areaId;
        this._sectionId = sectionId;
        this.position = position;
        this.rotation = rotation;
    }
}

export class QuestObject extends QuestEntity {
    @observable type: ObjectType;
    /**
     * The raw data from a DAT file.
     */
    dat: DatObject;

    constructor(
        areaId: number,
        sectionId: number,
        position: Vec3,
        rotation: Vec3,
        type: ObjectType,
        dat: DatObject
    ) {
        super(areaId, sectionId, position, rotation);

        if (!type) throw new Error('type is required.');

        this.type = type;
        this.dat = dat;
    }
}

export class QuestNpc extends QuestEntity {
    @observable type: NpcType;
    /**
     * The raw data from a DAT file.
     */
    dat: DatNpc;

    constructor(
        areaId: number,
        sectionId: number,
        position: Vec3,
        rotation: Vec3,
        type: NpcType,
        dat: DatNpc
    ) {
        super(areaId, sectionId, position, rotation);

        if (!type) throw new Error('type is required.');

        this.type = type;
        this.dat = dat;
    }
}

export class Area {
    id: number;
    name: string;
    order: number;
    areaVariants: AreaVariant[];

    constructor(id: number, name: string, order: number, areaVariants: AreaVariant[]) {
        if (!Number.isInteger(id) || id < 0)
            throw new Error(`Expected id to be a non-negative integer, got ${id}.`);
        if (!name) throw new Error('name is required.');
        if (!areaVariants) throw new Error('areaVariants is required.');

        this.id = id;
        this.name = name;
        this.order = order;
        this.areaVariants = areaVariants;
    }
}

export class AreaVariant {
    @observable sections: Section[] = [];

    constructor(public id: number, public area: Area) {
        if (!Number.isInteger(id) || id < 0)
            throw new Error(`Expected id to be a non-negative integer, got ${id}.`);
    }
}

type ItemDrop = {
    itemType: ItemType,
    anythingRate: number,
    rareRate: number
}

export class EnemyDrop implements ItemDrop {
    readonly rate: number;

    constructor(
        readonly difficulty: Difficulty,
        readonly sectionId: SectionId,
        readonly npcType: NpcType,
        readonly itemType: ItemType,
        readonly anythingRate: number,
        readonly rareRate: number
    ) {
        this.rate = anythingRate * rareRate;
    }
}

export class HuntMethod {
    readonly id: string;
    readonly name: string;
    readonly quest: SimpleQuest;
    readonly npcs: Array<SimpleNpc>;
    readonly enemies: Array<SimpleNpc>;
    readonly enemyCounts: Map<NpcType, number>;
    /**
     * The time it takes to complete the quest in hours.
     */
    readonly defaultTime: number;
    /**
     * The time it takes to complete the quest in hours as specified by the user.
     */
    @observable userTime?: number;

    @computed get time(): number {
        return this.userTime != null ? this.userTime : this.defaultTime;
    }

    constructor(
        id: string,
        name: string,
        quest: SimpleQuest,
        defaultTime: number
    ) {
        if (!id) throw new Error('id is required.');
        if (defaultTime <= 0) throw new Error('defaultTime must be greater than zero.');
        if (!name) throw new Error('name is required.');
        if (!quest) throw new Error('quest is required.');

        this.id = id;
        this.name = name;
        this.quest = quest;
        this.npcs = this.quest.npcs;
        this.enemies = this.npcs.filter(npc => npc.type.enemy);
        this.enemyCounts = new Map();

        for (const npc of this.enemies) {
            this.enemyCounts.set(npc.type, (this.enemyCounts.get(npc.type) || 0) + 1);
        }

        this.defaultTime = defaultTime;
    }
}

export class SimpleQuest {
    constructor(
        public readonly id: number,
        public readonly name: string,
        public readonly npcs: SimpleNpc[]
    ) {
        if (!id) throw new Error('id is required.');
        if (!name) throw new Error('name is required.');
        if (!npcs) throw new Error('npcs is required.');
    }
}

export class SimpleNpc {
    constructor(
        public type: NpcType
    ) {
        if (!type) throw new Error('type is required.');
    }
}
