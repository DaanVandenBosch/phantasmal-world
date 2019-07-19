import { BufferGeometry, Float32BufferAttribute, Uint16BufferAttribute, Vector3 } from "three";

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
    private bone_indices: number[] = [];
    private bone_weights: number[] = [];
    private groups: VertexGroup[] = [];
    private _max_material_index?: number;

    get vertex_count(): number {
        return this.positions.length / 3;
    }

    get index_count(): number {
        return this.indices.length;
    }

    get max_material_index(): number | undefined {
        return this._max_material_index;
    }

    get_position(index: number): Vector3 {
        return new Vector3(
            this.positions[3 * index],
            this.positions[3 * index + 1],
            this.positions[3 * index + 2]
        );
    }

    get_normal(index: number): Vector3 {
        return new Vector3(
            this.normals[3 * index],
            this.normals[3 * index + 1],
            this.normals[3 * index + 2]
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

    add_bone(index: number, weight: number): void {
        this.bone_indices.push(index);
        this.bone_weights.push(weight);
    }

    add_group(offset: number, size: number, material_id?: number): void {
        const last_group = this.groups[this.groups.length - 1];
        const mat_idx = material_id == null ? 0 : material_id + 1;

        if (last_group && last_group.material_index === mat_idx) {
            last_group.size += size;
        } else {
            this.groups.push({
                offset,
                size,
                material_index: mat_idx,
            });

            this._max_material_index = this._max_material_index
                ? Math.max(this._max_material_index, mat_idx)
                : mat_idx;
        }
    }

    build(): BufferGeometry {
        const geom = new BufferGeometry();

        geom.addAttribute("position", new Float32BufferAttribute(this.positions, 3));
        geom.addAttribute("normal", new Float32BufferAttribute(this.normals, 3));
        geom.addAttribute("uv", new Float32BufferAttribute(this.uvs, 2));

        geom.setIndex(new Uint16BufferAttribute(this.indices, 1));

        for (const group of this.groups) {
            geom.addGroup(group.offset, group.size, group.material_index);
        }

        if (this.bone_indices.length) {
            geom.addAttribute("skinIndex", new Uint16BufferAttribute(this.bone_indices, 4));
            geom.addAttribute("skinWeight", new Float32BufferAttribute(this.bone_weights, 4));
        }

        geom.computeBoundingSphere();
        geom.computeBoundingBox();

        return geom;
    }
}
