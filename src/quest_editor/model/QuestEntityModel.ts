import { EntityType } from "../../core/data_formats/parsing/quest/entities";
import { Vec3 } from "../../core/data_formats/vector";
import { Property } from "../../core/observable/property/Property";
import { map, property } from "../../core/observable";
import { WritableProperty } from "../../core/observable/property/WritableProperty";
import { SectionModel } from "./SectionModel";

export abstract class QuestEntityModel<Type extends EntityType = EntityType> {
    readonly type: Type;

    readonly area_id: number;

    readonly section_id: Property<number>;

    readonly section: Property<SectionModel | undefined>;

    set_section(section: SectionModel): this {
        this._section.val = section;
        this._section_id.val = section.id;
        return this;
    }

    /**
     * Section-relative position
     */
    readonly position: Property<Vec3>;

    set_position(position: Vec3): void {
        this._position.val = position;
    }

    readonly rotation: Property<Vec3>;

    set_rotation(rotation: Vec3): void {
        this._rotation.val = rotation;
    }

    /**
     * World position
     */
    readonly world_position: Property<Vec3>;

    set_world_position(pos: Vec3): this {
        let { x, y, z } = pos;
        const section = this.section.val;

        if (section) {
            const rel_x = x - section.position.x;
            const rel_y = y - section.position.y;
            const rel_z = z - section.position.z;
            const sin = -section.sin_y_axis_rotation;
            const cos = section.cos_y_axis_rotation;
            const rot_x = cos * rel_x + sin * rel_z;
            const rot_z = -sin * rel_x + cos * rel_z;
            x = rot_x;
            y = rel_y;
            z = rot_z;
        }

        this._position.val = new Vec3(x, y, z);
        return this;
    }

    private readonly _section_id: WritableProperty<number>;
    private readonly _section: WritableProperty<SectionModel | undefined> = property(undefined);
    private readonly _position: WritableProperty<Vec3>;
    private readonly _rotation: WritableProperty<Vec3>;

    protected constructor(
        type: Type,
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
    ) {
        this.type = type;
        this.area_id = area_id;
        this.section = this._section;
        this._section_id = property(section_id);
        this.section_id = this._section_id;
        this._position = property(position);
        this.position = this._position;
        this._rotation = property(rotation);
        this.rotation = this._rotation;
        this.world_position = map(this.position_to_world_position, this.section, this.position);
    }

    private position_to_world_position = (
        section: SectionModel | undefined,
        position: Vec3,
    ): Vec3 => {
        if (section) {
            let { x: rel_x, y: rel_y, z: rel_z } = position;

            const sin = -section.sin_y_axis_rotation;
            const cos = section.cos_y_axis_rotation;
            const rot_x = cos * rel_x - sin * rel_z;
            const rot_z = sin * rel_x + cos * rel_z;
            const x = rot_x + section.position.x;
            const y = rel_y + section.position.y;
            const z = rot_z + section.position.z;
            return new Vec3(x, y, z);
        } else {
            return position;
        }
    };
}
