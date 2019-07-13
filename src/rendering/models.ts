import {
    Bone,
    BufferGeometry,
    DoubleSide,
    Euler,
    Float32BufferAttribute,
    Material,
    Matrix3,
    Matrix4,
    MeshLambertMaterial,
    Quaternion,
    Skeleton,
    SkinnedMesh,
    Uint16BufferAttribute,
    Vector3,
    MeshBasicMaterial,
    Mesh,
} from "three";
import { vec3_to_threejs } from ".";
import { is_njcm_model, NjModel, NjObject } from "../data_formats/parsing/ninja";
import { NjcmModel } from "../data_formats/parsing/ninja/njcm";
import { xj_model_to_geometry } from "./xj_model_to_geometry";

const DUMMY_MATERIAL = new MeshBasicMaterial({
    color: 0x00ff00,
    side: DoubleSide,
});
const DEFAULT_MATERIAL = new MeshBasicMaterial({
    color: 0xff00ff,
    side: DoubleSide,
});
const DEFAULT_SKINNED_MATERIAL = new MeshLambertMaterial({
    skinning: true,
    color: 0xff00ff,
    side: DoubleSide,
});
const DEFAULT_NORMAL = new Vector3(0, 1, 0);
const NO_TRANSLATION = new Vector3(0, 0, 0);
const NO_ROTATION = new Quaternion(0, 0, 0, 1);
const NO_SCALE = new Vector3(1, 1, 1);

export function ninja_object_to_buffer_geometry(object: NjObject<NjModel>): BufferGeometry {
    return new Object3DCreator([]).create_buffer_geometry(object);
}

export function ninja_object_to_mesh(object: NjObject<NjModel>, materials: Material[] = []): Mesh {
    return new Object3DCreator(materials).create_mesh(object);
}

export function ninja_object_to_skinned_mesh(
    object: NjObject<NjModel>,
    materials: Material[] = []
): SkinnedMesh {
    return new Object3DCreator(materials).create_skinned_mesh(object);
}

export type VertexGroup = {
    /**
     * Start index.
     */
    start: number;
    count: number;
    material_index: number;
};

type Vertex = {
    bone_id: number;
    position: Vector3;
    normal?: Vector3;
    bone_weight: number;
    bone_weight_status: number;
    calc_continue: boolean;
};

class VerticesHolder {
    private vertices_stack: Vertex[][] = [];

    put(vertices: Vertex[]): void {
        this.vertices_stack.push(vertices);
    }

    get(index: number): Vertex[] {
        const vertices: Vertex[] = [];

        for (let i = this.vertices_stack.length - 1; i >= 0; i--) {
            const vertex = this.vertices_stack[i][index];

            if (vertex) {
                vertices.push(vertex);
            }
        }

        return vertices;
    }
}

class Object3DCreator {
    private materials: Material[];
    private vertices = new VerticesHolder();
    private bone_id: number = 0;
    private bones: Bone[] = [];

    private positions: number[] = [];
    private normals: number[] = [];
    private uvs: number[] = [];
    private indices: number[] = [];
    private bone_indices: number[] = [];
    private bone_weights: number[] = [];
    private groups: VertexGroup[] = [];

    constructor(materials: Material[]) {
        this.materials = [DUMMY_MATERIAL, ...materials];
    }

    create_buffer_geometry(object: NjObject<NjModel>): BufferGeometry {
        this.object_to_geometry(object, undefined, new Matrix4());

        const geom = new BufferGeometry();

        geom.addAttribute("position", new Float32BufferAttribute(this.positions, 3));
        geom.addAttribute("normal", new Float32BufferAttribute(this.normals, 3));
        geom.addAttribute("uv", new Float32BufferAttribute(this.uvs, 2));

        geom.setIndex(new Uint16BufferAttribute(this.indices, 1));

        for (const group of this.groups) {
            geom.addGroup(group.start, group.count, group.material_index);
        }

        geom.computeBoundingBox();

        return geom;
    }

    create_mesh(object: NjObject<NjModel>): Mesh {
        const geom = this.create_buffer_geometry(object);

        const max_mat_idx = this.groups.reduce((max, g) => Math.max(max, g.material_index), 0);

        for (let i = this.materials.length - 1; i < max_mat_idx; ++i) {
            this.materials.push(DEFAULT_MATERIAL);
        }

        return new Mesh(geom, this.materials);
    }

    create_skinned_mesh(object: NjObject<NjModel>): SkinnedMesh {
        const geom = this.create_buffer_geometry(object);

        geom.addAttribute("skinIndex", new Uint16BufferAttribute(this.bone_indices, 4));
        geom.addAttribute("skinWeight", new Float32BufferAttribute(this.bone_weights, 4));

        const max_mat_idx = this.groups.reduce((max, g) => Math.max(max, g.material_index), 0);

        for (let i = this.materials.length - 1; i < max_mat_idx; ++i) {
            this.materials.push(DEFAULT_SKINNED_MATERIAL);
        }

        const mesh = new SkinnedMesh(geom, this.materials);
        mesh.add(this.bones[0]);
        mesh.bind(new Skeleton(this.bones));

        return mesh;
    }

    private object_to_geometry(
        object: NjObject<NjModel>,
        parent_bone: Bone | undefined,
        parent_matrix: Matrix4
    ): void {
        const {
            no_translate,
            no_rotate,
            no_scale,
            hidden,
            break_child_trace,
            zxy_rotation_order,
            skip,
        } = object.evaluation_flags;
        const { position, rotation, scale } = object;

        const euler = new Euler(
            rotation.x,
            rotation.y,
            rotation.z,
            zxy_rotation_order ? "ZXY" : "ZYX"
        );
        const matrix = new Matrix4()
            .compose(
                no_translate ? NO_TRANSLATION : vec3_to_threejs(position),
                no_rotate ? NO_ROTATION : new Quaternion().setFromEuler(euler),
                no_scale ? NO_SCALE : vec3_to_threejs(scale)
            )
            .premultiply(parent_matrix);

        let bone: Bone | undefined;

        if (skip) {
            bone = parent_bone;
        } else {
            bone = new Bone();
            bone.name = this.bone_id.toString();

            bone.position.set(position.x, position.y, position.z);
            bone.setRotationFromEuler(euler);
            bone.scale.set(scale.x, scale.y, scale.z);

            this.bones.push(bone);

            if (parent_bone) {
                parent_bone.add(bone);
            }
        }

        if (object.model && !hidden) {
            this.model_to_geometry(object.model, matrix);
        }

        this.bone_id++;

        if (!break_child_trace) {
            for (const child of object.children) {
                this.object_to_geometry(child, bone, matrix);
            }
        }
    }

    private model_to_geometry(model: NjModel, matrix: Matrix4): void {
        if (is_njcm_model(model)) {
            this.njcm_model_to_geometry(model, matrix);
        } else {
            xj_model_to_geometry(
                model,
                matrix,
                this.positions,
                this.normals,
                this.uvs,
                this.indices,
                this.groups
            );
        }
    }

    private njcm_model_to_geometry(model: NjcmModel, matrix: Matrix4): void {
        const normal_matrix = new Matrix3().getNormalMatrix(matrix);

        const new_vertices = model.vertices.map(vertex => {
            const position = vec3_to_threejs(vertex.position);
            const normal = vertex.normal ? vec3_to_threejs(vertex.normal) : DEFAULT_NORMAL;

            position.applyMatrix4(matrix);
            normal.applyMatrix3(normal_matrix);

            return {
                bone_id: this.bone_id,
                position,
                normal,
                bone_weight: vertex.bone_weight,
                bone_weight_status: vertex.bone_weight_status,
                calc_continue: vertex.calc_continue,
            };
        });

        this.vertices.put(new_vertices);

        for (const mesh of model.meshes) {
            const start_index_count = this.indices.length;

            for (let i = 0; i < mesh.vertices.length; ++i) {
                const mesh_vertex = mesh.vertices[i];
                const vertices = this.vertices.get(mesh_vertex.index);

                if (vertices.length) {
                    const vertex = vertices[0];
                    const normal = vertex.normal || mesh_vertex.normal || DEFAULT_NORMAL;
                    const index = this.positions.length / 3;

                    this.positions.push(vertex.position.x, vertex.position.y, vertex.position.z);
                    this.normals.push(normal.x, normal.y, normal.z);

                    if (mesh.has_tex_coords) {
                        this.uvs.push(mesh_vertex.tex_coords!.x, mesh_vertex.tex_coords!.y);
                    } else {
                        this.uvs.push(0, 0);
                    }

                    if (i >= 2) {
                        if (i % 2 === (mesh.clockwise_winding ? 1 : 0)) {
                            this.indices.push(index - 2);
                            this.indices.push(index - 1);
                            this.indices.push(index);
                        } else {
                            this.indices.push(index - 2);
                            this.indices.push(index);
                            this.indices.push(index - 1);
                        }
                    }

                    const bone_indices = [0, 0, 0, 0];
                    const bone_weights = [0, 0, 0, 0];

                    for (let j = vertices.length - 1; j >= 0; j--) {
                        const vertex = vertices[j];
                        bone_indices[vertex.bone_weight_status] = vertex.bone_id;
                        bone_weights[vertex.bone_weight_status] = vertex.bone_weight;
                    }

                    this.bone_indices.push(...bone_indices);
                    this.bone_weights.push(...bone_weights);
                }
            }

            const last_group = this.groups[this.groups.length - 1];
            const mat_idx = mesh.texture_id == null ? 0 : mesh.texture_id + 1;

            if (last_group && last_group.material_index === mat_idx) {
                last_group.count += this.indices.length - start_index_count;
            } else {
                this.groups.push({
                    start: start_index_count,
                    count: this.indices.length - start_index_count,
                    material_index: mat_idx,
                });
            }
        }
    }
}
