import { computed, observable } from "mobx";
import { Object3D } from "three";
import { DatNpc, DatObject, DatUnknown } from "../data_formats/parsing/quest/dat";
import { Vec3 } from "../data_formats/Vec3";
import { enum_values } from "../enums";
import { ItemType } from "./items";
import { NpcType } from "./NpcType";
import { ObjectType } from "./ObjectType";

export * from "./items";
export * from "./NpcType";
export * from "./ObjectType";

export const RARE_ENEMY_PROB = 1 / 512;
export const KONDRIEU_PROB = 1 / 10;

export enum Server {
    Ephinea = "Ephinea",
}

export const Servers: Server[] = enum_values(Server);

export enum Episode {
    I = 1,
    II = 2,
    IV = 4,
}

export const Episodes: Episode[] = enum_values(Episode);

export function check_episode(episode: Episode): void {
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
    Normal,
    Hard,
    VHard,
    Ultimate,
}

export const Difficulties: Difficulty[] = enum_values(Difficulty);

export class Section {
    readonly id: number;
    readonly position: Vec3;
    readonly y_axis_rotation: number;
    readonly sin_y_axis_rotation: number;
    readonly cos_y_axis_rotation: number;

    constructor(id: number, position: Vec3, y_axis_rotation: number) {
        if (!Number.isInteger(id) || id < -1)
            throw new Error(`Expected id to be an integer greater than or equal to -1, got ${id}.`);
        if (!position) throw new Error("position is required.");
        if (typeof y_axis_rotation !== "number") throw new Error("y_axis_rotation is required.");

        this.id = id;
        this.position = position;
        this.y_axis_rotation = y_axis_rotation;
        this.sin_y_axis_rotation = Math.sin(this.y_axis_rotation);
        this.cos_y_axis_rotation = Math.cos(this.y_axis_rotation);
    }
}

export class Quest {
    @observable id?: number;
    @observable name: string;
    @observable short_description: string;
    @observable long_description: string;
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
    bin_data: ArrayBuffer;

    constructor(
        id: number | undefined,
        name: string,
        short_description: string,
        long_description: string,
        episode: Episode,
        area_variants: AreaVariant[],
        objects: QuestObject[],
        npcs: QuestNpc[],
        dat_unknowns: DatUnknown[],
        bin_data: ArrayBuffer
    ) {
        if (id != null && (!Number.isInteger(id) || id < 0))
            throw new Error("id should be undefined or a non-negative integer.");
        check_episode(episode);
        if (!objects || !(objects instanceof Array)) throw new Error("objs is required.");
        if (!npcs || !(npcs instanceof Array)) throw new Error("npcs is required.");

        this.id = id;
        this.name = name;
        this.short_description = short_description;
        this.long_description = long_description;
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
            const rel_x = x - this.section.position.x;
            const rel_y = y - this.section.position.y;
            const rel_z = z - this.section.position.z;
            const sin = -this.section.sin_y_axis_rotation;
            const cos = this.section.cos_y_axis_rotation;
            const rot_x = cos * rel_x + sin * rel_z;
            const rot_z = -sin * rel_x + cos * rel_z;
            x = rot_x;
            y = rel_y;
            z = rot_z;
        }

        return new Vec3(x, y, z);
    }

    set section_position(sec_pos: Vec3) {
        let { x: rel_x, y: rel_y, z: rel_z } = sec_pos;

        if (this.section) {
            const sin = -this.section.sin_y_axis_rotation;
            const cos = this.section.cos_y_axis_rotation;
            const rot_x = cos * rel_x - sin * rel_z;
            const rot_z = sin * rel_x + cos * rel_z;
            const x = rot_x + this.section.position.x;
            const y = rel_y + this.section.position.y;
            const z = rot_z + this.section.position.z;
            this.position = new Vec3(x, y, z);
        }
    }

    @observable object_3d?: Object3D;

    constructor(area_id: number, section_id: number, position: Vec3, rotation: Vec3) {
        if (Object.getPrototypeOf(this) === Object.getPrototypeOf(QuestEntity))
            throw new Error("Abstract class should not be instantiated directly.");
        if (!Number.isInteger(area_id) || area_id < 0)
            throw new Error(`Expected area_id to be a non-negative integer, got ${area_id}.`);
        if (!Number.isInteger(section_id) || section_id < 0)
            throw new Error(`Expected section_id to be a non-negative integer, got ${section_id}.`);
        if (!position) throw new Error("position is required.");
        if (!rotation) throw new Error("rotation is required.");

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

        if (!type) throw new Error("type is required.");

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

        if (!type) throw new Error("type is required.");

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
        if (!name) throw new Error("name is required.");
        if (!area_variants) throw new Error("area_variants is required.");

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
    item_type: ItemType;
    anything_rate: number;
    rare_rate: number;
};

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

    constructor(id: string, name: string, quest: SimpleQuest, default_time: number) {
        if (!id) throw new Error("id is required.");
        if (default_time <= 0) throw new Error("default_time must be greater than zero.");
        if (!name) throw new Error("name is required.");
        if (!quest) throw new Error("quest is required.");

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
        if (!id) throw new Error("id is required.");
        if (!name) throw new Error("name is required.");
        if (!enemy_counts) throw new Error("enemyCounts is required.");
    }
}

export class PlayerModel {
    constructor(
        public readonly name: string,
        public readonly head_style_count: number,
        public readonly hair_styles_count: number,
        public readonly hair_styles_with_accessory: Set<number>
    ) {}
}

export class PlayerAnimation {
    constructor(public readonly id: number, public readonly name: string) {}
}
