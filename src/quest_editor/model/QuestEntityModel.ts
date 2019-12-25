import { EntityType } from "../../core/data_formats/parsing/quest/entities";
import { Property } from "../../core/observable/property/Property";
import { property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { SectionModel } from "./SectionModel";
import { Euler, Quaternion, Vector3 } from "three";
import { floor_mod } from "../../core/math";
import { defined, require_integer } from "../../core/util";
import { euler_from_quat } from "./euler";

// These quaternions are used as temporary variables to avoid memory allocation.
const q1 = new Quaternion();
const q2 = new Quaternion();

export abstract class QuestEntityModel<Type extends EntityType = EntityType> {
    private readonly _section_id: WritableProperty<number>;
    private readonly _section: WritableProperty<SectionModel | undefined> = property(undefined);
    private readonly _position: WritableProperty<Vector3>;
    private readonly _world_position: WritableProperty<Vector3>;
    private readonly _rotation: WritableProperty<Euler>;
    private readonly _world_rotation: WritableProperty<Euler>;

    readonly type: Type;

    readonly area_id: number;

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

    protected constructor(
        type: Type,
        area_id: number,
        section_id: number,
        position: Vector3,
        rotation: Euler,
    ) {
        defined(type, "type");
        require_integer(area_id, "area_id");
        require_integer(section_id, "section_id");
        defined(position, "position");
        defined(rotation, "rotation");

        this.type = type;
        this.area_id = area_id;
        this.section = this._section;

        this._section_id = property(section_id);
        this.section_id = this._section_id;

        this._position = property(position);
        this.position = this._position;

        this._world_position = property(position);
        this.world_position = this._world_position;

        this._rotation = property(rotation);
        this.rotation = this._rotation;

        this._world_rotation = property(rotation);
        this.world_rotation = this._world_rotation;
    }

    set_section(section: SectionModel): this {
        if (section.area_variant.area.id !== this.area_id) {
            throw new Error(`Quest entities can't be moved across areas.`);
        }

        this._section.val = section;
        this._section_id.val = section.id;

        this.set_position(this.position.val);
        this.set_rotation(this.rotation.val);

        return this;
    }

    set_position(pos: Vector3): this {
        this._position.val = pos;

        const section = this.section.val;

        if (section) {
            this._world_position.val = pos
                .clone()
                .applyEuler(section.rotation)
                .add(section.position);
        } else {
            this._world_position.val = pos;
        }

        return this;
    }

    set_world_position(pos: Vector3): this {
        this._world_position.val = pos;

        const section = this.section.val;

        if (section) {
            this._position.val = pos
                .clone()
                .sub(section.position)
                .applyEuler(section.inverse_rotation);
        } else {
            this._position.val = pos;
        }

        return this;
    }

    set_rotation(rot: Euler): this {
        floor_mod_euler(rot);

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

        if (section) {
            q1.setFromEuler(rot);
            q2.setFromEuler(section.rotation);
            q2.inverse();

            this._rotation.val = floor_mod_euler(euler_from_quat(q1.multiply(q2)));
        } else {
            this._rotation.val = rot;
        }

        return this;
    }
}

function floor_mod_euler(euler: Euler): Euler {
    return euler.set(
        floor_mod(euler.x, 2 * Math.PI),
        floor_mod(euler.y, 2 * Math.PI),
        floor_mod(euler.z, 2 * Math.PI),
    );
}
