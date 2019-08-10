import {
    BufferGeometry,
    Float32BufferAttribute,
    Uint16BufferAttribute,
    Vector3,
    Bone,
} from "three";

export type BuilderData = {
    created_by_geometry_builder: boolean;
    /**
     * Maps material indices to normalized material indices.
     */
    normalized_material_indices: Map<number, number>;
    bones: Bone[];
};

export type BuilderVec2 = {
    x: number;
    y: number;
};

export type BuilderVec3 = {
    x: number;
    y: number;
    z: number;
};

type VertexGroup = {
    offset: number;
    size: number;
    material_index: number;
};

export class GeometryBuilder {
    private positions: number[] = [];
    private normals: number[] = [];
    private uvs: number[] = [];
    private indices: number[] = [];
    private bones: Bone[] = [];
    private bone_indices: number[] = [];
    private bone_weights: number[] = [];
    private groups: VertexGroup[] = [];
    /**
     * Will contain all material indices used in {@link this.groups} and -1 for the dummy material.
     */
    private material_indices = new Set<number>([-1]);

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

    add_group(offset: number, size: number, material_index?: number): void {
        const last_group = this.groups[this.groups.length - 1];
        const mat_idx = material_index == null ? -1 : material_index;

        if (last_group && last_group.material_index === mat_idx) {
            last_group.size += size;
        } else {
            this.groups.push({
                offset,
                size,
                material_index: mat_idx,
            });
            this.material_indices.add(mat_idx);
        }
    }

    build(): BufferGeometry {
        const geom = new BufferGeometry();
        const data = geom.userData as BuilderData;
        data.created_by_geometry_builder = true;

        geom.addAttribute("position", new Float32BufferAttribute(this.positions, 3));
        geom.addAttribute("normal", new Float32BufferAttribute(this.normals, 3));
        geom.addAttribute("uv", new Float32BufferAttribute(this.uvs, 2));

        geom.setIndex(new Uint16BufferAttribute(this.indices, 1));

        if (this.bone_indices.length && this.bones.length) {
            geom.addAttribute("skinIndex", new Uint16BufferAttribute(this.bone_indices, 4));
            geom.addAttribute("skinWeight", new Float32BufferAttribute(this.bone_weights, 4));
            data.bones = this.bones;
        } else {
            data.bones = [];
        }

        // Normalize material indices.
        const normalized_mat_idxs = new Map<number, number>();
        let i = 0;

        for (const mat_idx of [...this.material_indices].sort((a, b) => a - b)) {
            normalized_mat_idxs.set(mat_idx, i++);
        }

        // Use normalized material indices in Three.js groups.
        for (const group of this.groups) {
            geom.addGroup(group.offset, group.size, normalized_mat_idxs.get(group.material_index));
        }

        data.normalized_material_indices = normalized_mat_idxs;

        geom.computeBoundingSphere();
        geom.computeBoundingBox();

        return geom;
    }
}
