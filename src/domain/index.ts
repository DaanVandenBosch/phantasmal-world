import { computed, observable } from 'mobx';
import { Object3D } from 'three';
import { BufferCursor } from '../bin_data/BufferCursor';
import { DatNpc, DatObject, DatUnknown } from '../bin_data/parsing/quest/dat';
import { NpcType } from './NpcType';
import { ObjectType } from './ObjectType';
import { enumValues as enum_values } from '../enums';
import { ItemType } from './items';

export * from './items';
export * from './NpcType';
export * from './ObjectType';

export const RARE_ENEMY_PROB = 1 / 512;
export const KONDRIEU_PROB = 1 / 10;

export enum Server {
    Ephinea = 'Ephinea'
}

export const Servers: Server[] = enum_values(Server);

export enum Episode {
    I = 1,
    II = 2,
    IV = 4
}

export const Episodes: Episode[] = enum_values(Episode);

export function check_episode(episode: Episode) {
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

export const SectionIds: SectionId[] = enum_values(SectionId);

export enum Difficulty {
    Normal, Hard, VHard, Ultimate
}

export const Difficulties: Difficulty[] = enum_values(Difficulty);

export class Vec3 {
    x: number;
    y: number;
    z: number;

    constructor(x: number, y: number, z: number) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    add(v: Vec3): Vec3 {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    clone() {
        return new Vec3(this.x, this.y, this.z);
    }
};

export class Section {
    id: number;
    @observable position: Vec3;
    @observable y_axis_rotation: number;

    @computed get sin_y_axis_rotation(): number {
        return Math.sin(this.y_axis_rotation);
    }

    @computed get cos_y_axis_rotation(): number {
        return Math.cos(this.y_axis_rotation);
    }

    constructor(
        id: number,
        position: Vec3,
        y_axis_rotation: number
    ) {
        if (!Number.isInteger(id) || id < -1)
            throw new Error(`Expected id to be an integer greater than or equal to -1, got ${id}.`);
        if (!position) throw new Error('position is required.');
        if (typeof y_axis_rotation !== 'number') throw new Error('y_axis_rotation is required.');

        this.id = id;
        this.position = position;
        this.y_axis_rotation = y_axis_rotation;
    }
}

export class Quest {
    @observable name: string;
    @observable short_description: string;
    @observable long_description: string;
    @observable quest_no?: number;
    @observable episode: Episode;
    @observable area_variants: AreaVariant[];
    @observable objects: QuestObject[];
    @observable npcs: QuestNpc[];
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    dat_unknowns: DatUnknown[];
    /**
     * (Partial) raw BIN data that can't be parsed yet by Phantasmal.
     */
    bin_data: BufferCursor;

    constructor(
        name: string,
        short_description: string,
        long_description: string,
        quest_no: number | undefined,
        episode: Episode,
        area_variants: AreaVariant[],
        objects: QuestObject[],
        npcs: QuestNpc[],
        dat_unknowns: DatUnknown[],
        bin_data: BufferCursor
    ) {
        if (quest_no != null && (!Number.isInteger(quest_no) || quest_no < 0)) throw new Error('quest_no should be null or a non-negative integer.');
        check_episode(episode);
        if (!objects || !(objects instanceof Array)) throw new Error('objs is required.');
        if (!npcs || !(npcs instanceof Array)) throw new Error('npcs is required.');

        this.name = name;
        this.short_description = short_description;
        this.long_description = long_description;
        this.quest_no = quest_no;
        this.episode = episode;
        this.area_variants = area_variants;
        this.objects = objects;
        this.npcs = npcs;
        this.dat_unknowns = dat_unknowns;
        this.bin_data = bin_data;
    }
}

/**
 * Abstract class from which QuestNpc and QuestObject derive.
 */
export class QuestEntity {
    @observable area_id: number;

    private _section_id: number;

    @computed get section_id(): number {
        return this.section ? this.section.id : this._section_id;
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
    @computed get section_position(): Vec3 {
        let { x, y, z } = this.position;

        if (this.section) {
            const relX = x - this.section.position.x;
            const relY = y - this.section.position.y;
            const relZ = z - this.section.position.z;
            const sin = -this.section.sin_y_axis_rotation;
            const cos = this.section.cos_y_axis_rotation;
            const rotX = cos * relX + sin * relZ;
            const rotZ = -sin * relX + cos * relZ;
            x = rotX;
            y = relY;
            z = rotZ;
        }

        return new Vec3(x, y, z);
    }

    set section_position(sectPos: Vec3) {
        let { x: relX, y: relY, z: relZ } = sectPos;

        if (this.section) {
            const sin = -this.section.sin_y_axis_rotation;
            const cos = this.section.cos_y_axis_rotation;
            const rotX = cos * relX - sin * relZ;
            const rotZ = sin * relX + cos * relZ;
            const x = rotX + this.section.position.x;
            const y = relY + this.section.position.y;
            const z = rotZ + this.section.position.z;
            this.position = new Vec3(x, y, z);
        }
    }

    object_3d?: Object3D;

    constructor(
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3
    ) {
        if (Object.getPrototypeOf(this) === Object.getPrototypeOf(QuestEntity))
            throw new Error('Abstract class should not be instantiated directly.');
        if (!Number.isInteger(area_id) || area_id < 0)
            throw new Error(`Expected area_id to be a non-negative integer, got ${area_id}.`);
        if (!Number.isInteger(section_id) || section_id < 0)
            throw new Error(`Expected section_id to be a non-negative integer, got ${section_id}.`);
        if (!position) throw new Error('position is required.');
        if (!rotation) throw new Error('rotation is required.');

        this.area_id = area_id;
        this._section_id = section_id;
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
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        type: ObjectType,
        dat: DatObject
    ) {
        super(area_id, section_id, position, rotation);

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
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        type: NpcType,
        dat: DatNpc
    ) {
        super(area_id, section_id, position, rotation);

        if (!type) throw new Error('type is required.');

        this.type = type;
        this.dat = dat;
    }
}

export class Area {
    id: number;
    name: string;
    order: number;
    area_variants: AreaVariant[];

    constructor(id: number, name: string, order: number, area_variants: AreaVariant[]) {
        if (!Number.isInteger(id) || id < 0)
            throw new Error(`Expected id to be a non-negative integer, got ${id}.`);
        if (!name) throw new Error('name is required.');
        if (!area_variants) throw new Error('area_variants is required.');

        this.id = id;
        this.name = name;
        this.order = order;
        this.area_variants = area_variants;
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
    item_type: ItemType,
    anything_rate: number,
    rare_rate: number
}

export class EnemyDrop implements ItemDrop {
    readonly rate: number;

    constructor(
        readonly difficulty: Difficulty,
        readonly section_id: SectionId,
        readonly npc_type: NpcType,
        readonly item_type: ItemType,
        readonly anything_rate: number,
        readonly rare_rate: number
    ) {
        this.rate = anything_rate * rare_rate;
    }
}

export class HuntMethod {
    readonly id: string;
    readonly name: string;
    readonly episode: Episode;
    readonly quest: SimpleQuest;
    readonly enemy_counts: Map<NpcType, number>;
    /**
     * The time it takes to complete the quest in hours.
     */
    readonly default_time: number;
    /**
     * The time it takes to complete the quest in hours as specified by the user.
     */
    @observable user_time?: number;

    @computed get time(): number {
        return this.user_time != null ? this.user_time : this.default_time;
    }

    constructor(
        id: string,
        name: string,
        quest: SimpleQuest,
        default_time: number
    ) {
        if (!id) throw new Error('id is required.');
        if (default_time <= 0) throw new Error('default_time must be greater than zero.');
        if (!name) throw new Error('name is required.');
        if (!quest) throw new Error('quest is required.');

        this.id = id;
        this.name = name;
        this.episode = quest.episode;
        this.quest = quest;
        this.enemy_counts = quest.enemy_counts;
        this.default_time = default_time;
    }
}

export class SimpleQuest {
    constructor(
        public readonly id: number,
        public readonly name: string,
        public readonly episode: Episode,
        public readonly enemy_counts: Map<NpcType, number>
    ) {
        if (!id) throw new Error('id is required.');
        if (!name) throw new Error('name is required.');
        if (!enemy_counts) throw new Error('enemyCounts is required.');
    }
}
