import { action, computed, observable } from "mobx";
import { EntityType } from "../data_formats/parsing/quest/entities";
import { Episode } from "../data_formats/parsing/quest/Episode";
import { Vec3 } from "../data_formats/vector";
import { enum_values } from "../enums";
import { ItemType } from "./items";
import { ObjectType } from "../data_formats/parsing/quest/object_types";
import { NpcType } from "../data_formats/parsing/quest/npc_types";

export * from "./items";

export const RARE_ENEMY_PROB = 1 / 512;
export const KONDRIEU_PROB = 1 / 10;

export enum Server {
    Ephinea = "Ephinea",
}

export const Servers: Server[] = enum_values(Server);

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
        if (!Number.isFinite(y_axis_rotation)) throw new Error("y_axis_rotation is required.");

        this.id = id;
        this.position = position;
        this.y_axis_rotation = y_axis_rotation;
        this.sin_y_axis_rotation = Math.sin(this.y_axis_rotation);
        this.cos_y_axis_rotation = Math.cos(this.y_axis_rotation);
    }
}

/**
 * Abstract class from which ObservableQuestNpc and ObservableQuestObject derive.
 */
export abstract class ObservableQuestEntity<Type extends EntityType = EntityType> {
    readonly type: Type;

    @observable area_id: number;

    private readonly _section_id: number;

    @computed get section_id(): number {
        return this.section ? this.section.id : this._section_id;
    }

    @observable.ref section?: Section;

    /**
     * Section-relative position
     */
    @observable.ref position: Vec3;

    @observable.ref rotation: Vec3;

    @observable.ref scale: Vec3;

    /**
     * World position
     */
    @computed get world_position(): Vec3 {
        if (this.section) {
            let { x: rel_x, y: rel_y, z: rel_z } = this.position;

            const sin = -this.section.sin_y_axis_rotation;
            const cos = this.section.cos_y_axis_rotation;
            const rot_x = cos * rel_x - sin * rel_z;
            const rot_z = sin * rel_x + cos * rel_z;
            const x = rot_x + this.section.position.x;
            const y = rel_y + this.section.position.y;
            const z = rot_z + this.section.position.z;
            return new Vec3(x, y, z);
        } else {
            return this.position;
        }
    }

    set world_position(pos: Vec3) {
        let { x, y, z } = pos;

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

        this.position = new Vec3(x, y, z);
    }

    protected constructor(
        type: Type,
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        scale: Vec3,
    ) {
        if (type == undefined) throw new Error("type is required.");
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
    set_world_position_and_section(world_position: Vec3, section?: Section): void {
        this.world_position = world_position;
        this.section = section;
    }
}

export class ObservableQuestObject extends ObservableQuestEntity<ObjectType> {
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
        unknown: number[][],
    ) {
        super(type, area_id, section_id, position, rotation, scale);

        this.type = type;
        this.unknown = unknown;
    }
}

export class ObservableQuestNpc extends ObservableQuestEntity<NpcType> {
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
        unknown: number[][],
    ) {
        super(type, area_id, section_id, position, rotation, scale);

        this.type = type;
        this.pso_type_id = pso_type_id;
        this.pso_skin = pso_skin;
        this.unknown = unknown;
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
        readonly rare_rate: number,
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
        readonly id: number,
        readonly name: string,
        readonly episode: Episode,
        readonly enemy_counts: Map<NpcType, number>,
    ) {
        if (!id) throw new Error("id is required.");
        if (!name) throw new Error("name is required.");
        if (!enemy_counts) throw new Error("enemyCounts is required.");
    }
}

export class PlayerModel {
    constructor(
        readonly name: string,
        readonly head_style_count: number,
        readonly hair_styles_count: number,
        readonly hair_styles_with_accessory: Set<number>,
    ) {}
}

export class PlayerAnimation {
    constructor(readonly id: number, readonly name: string) {}
}
