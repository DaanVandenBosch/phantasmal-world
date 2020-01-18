import {
    Bone,
    BufferGeometry,
    Float32BufferAttribute,
    Uint16BufferAttribute,
    Vector3,
} from "three";
import { map_get_or_put } from "../../util";

export type BuilderData = {
    readonly created_by_geometry_builder: boolean;
    readonly materials: BuilderMaterial[];
    readonly bones: Bone[];
};

export type BuilderVec2 = {
    readonly x: number;
    readonly y: number;
};

export type BuilderVec3 = {
    readonly x: number;
    readonly y: number;
    readonly z: number;
};

export type BuilderMaterial = {
    readonly texture_id?: number;
    readonly alpha: boolean;
    readonly additive_blending: boolean;
};

/**
 * Maps various material properties to material IDs.
 */
export class MaterialMap {
    private readonly materials: BuilderMaterial[] = [{ alpha: false, additive_blending: false }];
    private readonly map = new Map<number, number>();

    /**
     * Returns an index to an existing material if one exists for the given arguments. Otherwise
     * adds a new material and returns its index.
     */
    add_material(
        texture_id?: number,
        alpha: boolean = false,
        additive_blending: boolean = false,
    ): number {
        if (texture_id == undefined) {
            return 0;
        } else {
            const key = (texture_id << 2) | (alpha ? 0b10 : 0) | (additive_blending ? 1 : 0);
            return map_get_or_put(this.map, key, () => {
                this.materials.push({ texture_id, alpha, additive_blending });
                return this.materials.length - 1;
            });
        }
    }

    get_materials(): BuilderMaterial[] {
        return this.materials;
    }
}

type VertexGroup = {
    offset: number;
    size: number;
    material_index: number;
};

export class GeometryBuilder {
    private readonly positions: number[] = [];
    private readonly normals: number[] = [];
    private readonly uvs: number[] = [];
    private readonly indices: number[] = [];
    private readonly bones: Bone[] = [];
    private readonly bone_indices: number[] = [];
    private readonly bone_weights: number[] = [];
    private readonly groups: VertexGroup[] = [];
    /**
     * Will contain all material indices used in {@link this.groups} and -1 for the dummy material.
     */
    private readonly material_map = new MaterialMap();

    get vertex_count(): number {
        return this.positions.length / 3;
    }

    get index_count(): number {
        return this.indices.length;
    }

    get_position(index: number): Vector3 {
        return new Vector3(
            this.positions[3 * index],
            this.positions[3 * index + 1],
            this.positions[3 * index + 2],
        );
    }

    get_normal(index: number): Vector3 {
        return new Vector3(
            this.normals[3 * index],
            this.normals[3 * index + 1],
            this.normals[3 * index + 2],
        );
    }

    add_vertex(position: BuilderVec3, normal: BuilderVec3, uv: BuilderVec2): void {
        this.positions.push(position.x, position.y, position.z);
        this.normals.push(normal.x, normal.y, normal.z);
        this.uvs.push(uv.x, uv.y);
    }

    add_index(index: number): void {
        this.indices.push(index);
    }

    add_bone(bone: Bone): void {
        this.bones.push(bone);
    }

    add_bone_weight(index: number, weight: number): void {
        this.bone_indices.push(index);
        this.bone_weights.push(weight);
    }

    add_group(
        offset: number,
        size: number,
        texture_id?: number,
        alpha: boolean = false,
        additive_blending: boolean = false,
    ): void {
        const last_group = this.groups[this.groups.length - 1];
        const material_index = this.material_map.add_material(texture_id, alpha, additive_blending);

        if (last_group && last_group.material_index === material_index) {
            last_group.size += size;
        } else {
            this.groups.push({
                offset,
                size,
                material_index,
            });
        }
    }

    build(): BufferGeometry {
        const geom = new BufferGeometry();

        geom.setAttribute("position", new Float32BufferAttribute(this.positions, 3));
        geom.setAttribute("normal", new Float32BufferAttribute(this.normals, 3));
        geom.setAttribute("uv", new Float32BufferAttribute(this.uvs, 2));

        geom.setIndex(new Uint16BufferAttribute(this.indices, 1));

        let bones: Bone[];

        if (this.bone_indices.length && this.bones.length) {
            geom.setAttribute("skinIndex", new Uint16BufferAttribute(this.bone_indices, 4));
            geom.setAttribute("skinWeight", new Float32BufferAttribute(this.bone_weights, 4));
            bones = this.bones;
        } else {
            bones = [];
        }

        for (const group of this.groups) {
            geom.addGroup(group.offset, group.size, group.material_index);
        }

        // noinspection UnnecessaryLocalVariableJS
        const data: BuilderData = {
            created_by_geometry_builder: true,
            materials: this.material_map.get_materials(),
            bones,
        };

        geom.userData = data;

        geom.computeBoundingSphere();
        geom.computeBoundingBox();

        return geom;
    }
}
