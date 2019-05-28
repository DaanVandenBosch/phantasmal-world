import { Object3D } from 'three';
import { computed, observable } from 'mobx';
import { NpcType } from './NpcType';
import { ObjectType } from './ObjectType';

export { NpcType } from './NpcType';
export { ObjectType } from './ObjectType';

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
    @observable episode: number;
    @observable area_variants: AreaVariant[];
    @observable objects: QuestObject[];
    @observable npcs: QuestNpc[];
    /**
     * (Partial) raw DAT data that can't be parsed yet by Phantasmal.
     */
    dat: any;
    /**
     * (Partial) raw BIN data that can't be parsed yet by Phantasmal.
     */
    bin: any;

    constructor(
        name: string,
        short_description: string,
        long_description: string,
        quest_no: number | undefined,
        episode: number,
        area_variants: AreaVariant[],
        objects: QuestObject[],
        npcs: QuestNpc[],
        dat: any,
        bin: any
    ) {
        if (quest_no != null && (!Number.isInteger(quest_no) || quest_no < 0)) throw new Error('quest_no should be null or a non-negative integer.');
        if (episode !== 1 && episode !== 2 && episode !== 4) throw new Error('episode should be 1, 2 or 4.');
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
        this.dat = dat;
        this.bin = bin;
    }
}

export class VisibleQuestEntity {
    @observable area_id: number;

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

    set section_position(sect_pos: Vec3) {
        let { x: rel_x, y: rel_y, z: rel_z } = sect_pos;

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

    object3d?: Object3D;

    constructor(
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3
    ) {
        if (Object.getPrototypeOf(this) === Object.getPrototypeOf(VisibleQuestEntity))
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

    private _section_id: number;
}

export class QuestObject extends VisibleQuestEntity {
    @observable type: ObjectType;
    /**
     * The raw data from a DAT file.
     */
    dat: any;

    constructor(
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        type: ObjectType,
        dat: any
    ) {
        super(area_id, section_id, position, rotation);

        if (!type) throw new Error('type is required.');

        this.type = type;
        this.dat = dat;
    }
}

export class QuestNpc extends VisibleQuestEntity {
    @observable type: NpcType;
    /**
     * The raw data from a DAT file.
     */
    dat: any;

    constructor(
        area_id: number,
        section_id: number,
        position: Vec3,
        rotation: Vec3,
        type: NpcType,
        dat: any
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
