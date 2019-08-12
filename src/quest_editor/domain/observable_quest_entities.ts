import { ObjectType } from "../../core/data_formats/parsing/quest/object_types";
import { action, computed, observable } from "mobx";
import { Vec3 } from "../../core/data_formats/vector";
import { EntityType } from "../../core/data_formats/parsing/quest/entities";
import { Section } from "./Section";
import { NpcType } from "../../core/data_formats/parsing/quest/npc_types";

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
    readonly id: number;
    readonly group_id: number;
    readonly object_id: number;
    readonly action: number;

    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    readonly unknown: number[][];

    constructor(
        type: ObjectType,
        id: number,
        group_id: number,
        object_id: number,
        action: number,
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        scale: Vec3,
        unknown: number[][],
    ) {
        super(type, area_id, section_id, position, rotation, scale);

        this.id = id;
        this.group_id = group_id;
        this.object_id = object_id;
        this.action = action;
        this.unknown = unknown;
    }
}

export class ObservableQuestNpc extends ObservableQuestEntity<NpcType> {
    readonly pso_type_id: number;
    readonly pso_skin: number;
    /**
     * Data of which the purpose hasn't been discovered yet.
     */
    readonly unknown: number[][];

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

        this.pso_type_id = pso_type_id;
        this.pso_skin = pso_skin;
        this.unknown = unknown;
    }
}
