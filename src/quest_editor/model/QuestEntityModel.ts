import { EntityType, QuestEntity } from "../../core/data_formats/parsing/quest/Quest";
import { Property } from "../../core/observable/property/Property";
import { property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { SectionModel } from "./SectionModel";
import { Euler, Quaternion, Vector3 } from "three";
import { floor_mod } from "../../core/math";
import { euler, euler_from_quat } from "./euler";
import { vec3_to_threejs } from "../../core/rendering/conversion";
import { Vec3 } from "../../core/data_formats/vector";

// These quaternions are used as temporary variables to avoid memory allocation.
const q1 = new Quaternion();
const q2 = new Quaternion();

export abstract class QuestEntityModel<
    Type extends EntityType = EntityType,
    Entity extends QuestEntity = QuestEntity
> {
    private readonly _section_id: WritableProperty<number>;
    private readonly _section: WritableProperty<SectionModel | undefined> = property(undefined);
    private readonly _position: WritableProperty<Vector3>;
    private readonly _world_position: WritableProperty<Vector3>;
    private readonly _rotation: WritableProperty<Euler>;
    private readonly _world_rotation: WritableProperty<Euler>;

    /**
     * Many modifications done to the underlying entity directly will not be reflected in this
     * model's properties.
     */
    readonly entity: Entity;

    abstract readonly type: Type;

    get area_id(): number {
        return this.entity.area_id;
    }

    readonly section_id: Property<number>;

    readonly section: Property<SectionModel | undefined>;

    /**
     * Section-relative position
     */
    readonly position: Property<Vector3>;

    /**
     * World position
     */
    readonly world_position: Property<Vector3>;

    readonly rotation: Property<Euler>;

    readonly world_rotation: Property<Euler>;

    protected constructor(entity: Entity) {
        this.entity = entity;

        this.section = this._section;

        this._section_id = property(this.get_entity_section_id());
        this.section_id = this._section_id;

        const position = vec3_to_threejs(this.get_entity_position());

        this._position = property(position);
        this.position = this._position;

        this._world_position = property(position);
        this.world_position = this._world_position;

        const { x: rot_x, y: rot_y, z: rot_z } = this.get_entity_rotation();
        const rotation = euler(rot_x, rot_y, rot_z);

        this._rotation = property(rotation);
        this.rotation = this._rotation;

        this._world_rotation = property(rotation);
        this.world_rotation = this._world_rotation;
    }

    set_section(section: SectionModel): this {
        if (section.area_variant.area.id !== this.area_id) {
            throw new Error(`Quest entities can't be moved across areas.`);
        }

        this.set_entity_section_id(section.id);

        this._section.val = section;
        this._section_id.val = section.id;

        this.set_position(this.position.val);
        this.set_rotation(this.rotation.val);

        return this;
    }

    set_position(pos: Vector3): this {
        this.set_entity_position(pos);

        this._position.val = pos;

        const section = this.section.val;

        this._world_position.val = section
            ? pos.clone().applyEuler(section.rotation).add(section.position)
            : pos;

        return this;
    }

    set_world_position(pos: Vector3): this {
        this._world_position.val = pos;

        const section = this.section.val;

        const rel_pos = section
            ? pos.clone().sub(section.position).applyEuler(section.inverse_rotation)
            : pos;

        this.set_entity_position(rel_pos);
        this._position.val = rel_pos;

        return this;
    }

    set_rotation(rot: Euler): this {
        floor_mod_euler(rot);

        this.set_entity_rotation(rot);

        this._rotation.val = rot;

        const section = this.section.val;

        if (section) {
            q1.setFromEuler(rot);
            q2.setFromEuler(section.rotation);
            this._world_rotation.val = floor_mod_euler(euler_from_quat(q1.multiply(q2)));
        } else {
            this._world_rotation.val = rot;
        }

        return this;
    }

    set_world_rotation(rot: Euler): this {
        floor_mod_euler(rot);

        this._world_rotation.val = rot;

        const section = this.section.val;

        let rel_rot: Euler;

        if (section) {
            q1.setFromEuler(rot);
            q2.setFromEuler(section.rotation);
            q2.inverse();

            rel_rot = floor_mod_euler(euler_from_quat(q1.multiply(q2)));
        } else {
            rel_rot = rot;
        }

        this.set_entity_rotation(rel_rot);
        this._rotation.val = rel_rot;

        return this;
    }

    protected abstract get_entity_section_id(): number;
    protected abstract set_entity_section_id(section_id: number): void;

    protected abstract get_entity_position(): Vec3;
    protected abstract set_entity_position(position: Vec3): void;

    protected abstract get_entity_rotation(): Vec3;
    protected abstract set_entity_rotation(rotation: Vec3): void;
}

function floor_mod_euler(euler: Euler): Euler {
    return euler.set(
        floor_mod(euler.x, 2 * Math.PI),
        floor_mod(euler.y, 2 * Math.PI),
        floor_mod(euler.z, 2 * Math.PI),
    );
}
