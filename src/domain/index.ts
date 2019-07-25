import { action, computed, observable } from "mobx";
import { DatUnknown } from "../data_formats/parsing/quest/dat";
import { Vec3 } from "../data_formats/vector";
import { enum_values } from "../enums";
import { ItemType } from "./items";
import { NpcType } from "./NpcType";
import { ObjectType } from "./ObjectType";
import { Instruction } from "../data_formats/parsing/quest/bin";

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
    if (Episode[episode] == undefined) {
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
    @observable private _id!: number;

    get id(): number {
        return this._id;
    }

    @action
    set_id(id: number): void {
        if (!Number.isInteger(id) || id < 0 || id > 4294967295)
            throw new Error("id must be an integer greater than 0 and less than 4294967295.");
        this._id = id;
    }

    @observable private _language!: number;

    get language(): number {
        return this._language;
    }

    @action
    set_language(language: number): void {
        if (!Number.isInteger(language)) throw new Error("language must be an integer.");
        this._language = language;
    }

    @observable private _name!: string;

    get name(): string {
        return this._name;
    }

    @action
    set_name(name: string): void {
        if (name.length > 32) throw new Error("name can't be longer than 32 characters.");
        this._name = name;
    }

    @observable private _short_description!: string;

    get short_description(): string {
        return this._short_description;
    }

    @action
    set_short_description(short_description: string): void {
        if (short_description.length > 128)
            throw new Error("short_description can't be longer than 128 characters.");
        this._short_description = short_description;
    }

    @observable _long_description!: string;

    get long_description(): string {
        return this._long_description;
    }

    @action
    set_long_description(long_description: string): void {
        if (long_description.length > 288)
            throw new Error("long_description can't be longer than 288 characters.");
        this._long_description = long_description;
    }

    readonly episode: Episode;

    @observable readonly area_variants: AreaVariant[];
    @observable readonly objects: QuestObject[];
    @observable readonly npcs: QuestNpc[];
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    readonly dat_unknowns: DatUnknown[];
    readonly labels: Map<number, number>;
    readonly instructions: Instruction[];
    readonly shop_items: number[];

    constructor(
        id: number,
        language: number,
        name: string,
        short_description: string,
        long_description: string,
        episode: Episode,
        area_variants: AreaVariant[],
        objects: QuestObject[],
        npcs: QuestNpc[],
        dat_unknowns: DatUnknown[],
        labels: Map<number, number>,
        instructions: Instruction[],
        shop_items: number[]
    ) {
        check_episode(episode);
        if (!area_variants) throw new Error("area_variants is required.");
        if (!objects || !(objects instanceof Array)) throw new Error("objs is required.");
        if (!npcs || !(npcs instanceof Array)) throw new Error("npcs is required.");
        if (!dat_unknowns) throw new Error("dat_unknowns is required.");
        if (!labels) throw new Error("labels is required.");
        if (!instructions) throw new Error("instructions is required.");
        if (!shop_items) throw new Error("shop_items is required.");

        this.set_id(id);
        this.set_language(language);
        this.set_name(name);
        this.set_short_description(short_description);
        this.set_long_description(long_description);
        this.episode = episode;
        this.area_variants = area_variants;
        this.objects = objects;
        this.npcs = npcs;
        this.dat_unknowns = dat_unknowns;
        this.labels = labels;
        this.instructions = instructions;
        this.shop_items = shop_items;
    }
}

export interface EntityType {
    readonly id: number;
    readonly code: string;
    readonly name: string;
}

/**
 * Abstract class from which QuestNpc and QuestObject derive.
 */
export abstract class QuestEntity<Type extends EntityType = EntityType> {
    readonly type: Type;

    @observable area_id: number;

    private _section_id: number;

    @computed get section_id(): number {
        return this.section ? this.section.id : this._section_id;
    }

    @observable.ref section?: Section;

    /**
     * World position
     */
    @observable.ref position: Vec3;

    @observable.ref rotation: Vec3;

    @observable.ref scale: Vec3;

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

    constructor(
        type: Type,
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        scale: Vec3
    ) {
        if (!type) throw new Error("type is required.");
        if (!Number.isInteger(area_id) || area_id < 0)
            throw new Error(`Expected area_id to be a non-negative integer, got ${area_id}.`);
        if (!Number.isInteger(section_id) || section_id < 0)
            throw new Error(`Expected section_id to be a non-negative integer, got ${section_id}.`);
        if (!position) throw new Error("position is required.");
        if (!rotation) throw new Error("rotation is required.");
        if (!scale) throw new Error("scale is required.");

        this.type = type;
        this.area_id = area_id;
        this._section_id = section_id;
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    @action
    set_position_and_section(position: Vec3, section?: Section): void {
        this.position = position;
        this.section = section;
    }
}

export class QuestObject extends QuestEntity<ObjectType> {
    @observable type: ObjectType;
    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    unknown: number[][];

    constructor(
        type: ObjectType,
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        scale: Vec3,
        unknown: number[][]
    ) {
        super(type, area_id, section_id, position, rotation, scale);

        this.type = type;
        this.unknown = unknown;
    }
}

export class QuestNpc extends QuestEntity<NpcType> {
    @observable type: NpcType;
    pso_type_id: number;
    pso_skin: number;
    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    unknown: number[][];

    constructor(
        type: NpcType,
        pso_type_id: number,
        pso_skin: number,
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        scale: Vec3,
        unknown: number[][]
    ) {
        super(type, area_id, section_id, position, rotation, scale);

        this.type = type;
        this.pso_type_id = pso_type_id;
        this.pso_skin = pso_skin;
        this.unknown = unknown;
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
    @observable.shallow sections: Section[] = [];

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
