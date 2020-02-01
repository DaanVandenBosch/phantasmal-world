import { Bone, BufferGeometry, Euler, Matrix3, Matrix4, Quaternion, Vector2, Vector3 } from "three";
import { vec3_to_threejs } from "./index";
import { is_njcm_model, NjModel, NjObject } from "../../data_formats/parsing/ninja";
import { NjcmModel } from "../../data_formats/parsing/ninja/njcm";
import { XjModel } from "../../data_formats/parsing/ninja/xj";
import { GeometryBuilder } from "./GeometryBuilder";

const DEFAULT_NORMAL = new Vector3(0, 1, 0);
const DEFAULT_UV = new Vector2(0, 0);
const NO_TRANSLATION = new Vector3(0, 0, 0);
const NO_ROTATION = new Quaternion(0, 0, 0, 1);
const NO_SCALE = new Vector3(1, 1, 1);

export function ninja_object_to_geometry_builder(object: NjObject, builder: GeometryBuilder): void {
    new GeometryCreator(builder).to_geometry_builder(object);
}

export function ninja_object_to_buffer_geometry(object: NjObject): BufferGeometry {
    return new GeometryCreator(new GeometryBuilder()).create_buffer_geometry(object);
}

type Vertex = {
    bone_id: number;
    position: Vector3;
    normal?: Vector3;
    bone_weight: number;
    bone_weight_status: number;
    calc_continue: boolean;
};

class VerticesHolder {
    private readonly vertices_stack: Vertex[][] = [];

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

class GeometryCreator {
    private readonly vertices = new VerticesHolder();
    private readonly builder: GeometryBuilder;
    private bone_id = 0;

    constructor(builder: GeometryBuilder) {
        this.builder = builder;
    }

    to_geometry_builder(object: NjObject): void {
        this.object_to_geometry(object, undefined, new Matrix4());
    }

    create_buffer_geometry(object: NjObject): BufferGeometry {
        this.to_geometry_builder(object);
        return this.builder.build();
    }

    private object_to_geometry(
        object: NjObject,
        parent_bone: Bone | undefined,
        parent_matrix: Matrix4,
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
            zxy_rotation_order ? "ZXY" : "ZYX",
        );
        const matrix = new Matrix4()
            .compose(
                no_translate ? NO_TRANSLATION : vec3_to_threejs(position),
                no_rotate ? NO_ROTATION : new Quaternion().setFromEuler(euler),
                no_scale ? NO_SCALE : vec3_to_threejs(scale),
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

            this.builder.add_bone(bone);

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
            this.xj_model_to_geometry(model, matrix);
        }
    }

    private njcm_model_to_geometry(model: NjcmModel, matrix: Matrix4): void {
        const normal_matrix = new Matrix3().getNormalMatrix(matrix);

        const new_vertices = model.vertices.map(vertex => {
            const position = vec3_to_threejs(vertex.position);
            const normal = vertex.normal ? vec3_to_threejs(vertex.normal) : new Vector3(0, 1, 0);

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
            const start_index_count = this.builder.index_count;

            for (let i = 0; i < mesh.vertices.length; ++i) {
                const mesh_vertex = mesh.vertices[i];
                const vertices = this.vertices.get(mesh_vertex.index);

                if (vertices.length) {
                    const vertex = vertices[0];
                    const normal = vertex.normal ?? mesh_vertex.normal ?? DEFAULT_NORMAL;
                    const index = this.builder.vertex_count;

                    this.builder.add_vertex(
                        vertex.position,
                        normal,
                        mesh.has_tex_coords ? mesh_vertex.tex_coords! : DEFAULT_UV,
                    );

                    if (i >= 2) {
                        if (i % 2 === (mesh.clockwise_winding ? 1 : 0)) {
                            this.builder.add_index(index - 2);
                            this.builder.add_index(index - 1);
                            this.builder.add_index(index);
                        } else {
                            this.builder.add_index(index - 2);
                            this.builder.add_index(index);
                            this.builder.add_index(index - 1);
                        }
                    }

                    const bones = [
                        [0, 0],
                        [0, 0],
                        [0, 0],
                        [0, 0],
                    ];

                    for (let j = vertices.length - 1; j >= 0; j--) {
                        const vertex = vertices[j];
                        bones[vertex.bone_weight_status] = [vertex.bone_id, vertex.bone_weight];
                    }

                    const total_weight = bones.reduce((total, [, weight]) => total + weight, 0);

                    for (const [bone_index, bone_weight] of bones) {
                        this.builder.add_bone_weight(
                            bone_index,
                            total_weight > 0 ? bone_weight / total_weight : bone_weight,
                        );
                    }
                }
            }

            this.builder.add_group(
                start_index_count,
                this.builder.index_count - start_index_count,
                mesh.texture_id,
                mesh.use_alpha,
                mesh.src_alpha !== 4 || mesh.dst_alpha !== 5,
            );
        }
    }

    private xj_model_to_geometry(model: XjModel, matrix: Matrix4): void {
        const index_offset = this.builder.vertex_count;
        const normal_matrix = new Matrix3().getNormalMatrix(matrix);

        for (const { position, normal, uv } of model.vertices) {
            const p = vec3_to_threejs(position).applyMatrix4(matrix);

            const local_n = normal ? vec3_to_threejs(normal) : new Vector3(0, 1, 0);
            const n = local_n.applyMatrix3(normal_matrix);

            const tuv = uv || DEFAULT_UV;

            this.builder.add_vertex(p, n, tuv);
        }

        let current_mat_idx: number | undefined;
        let current_src_alpha: number | undefined;
        let current_dst_alpha: number | undefined;

        for (const mesh of model.meshes) {
            const start_index_count = this.builder.index_count;
            let clockwise = false;

            for (let j = 2; j < mesh.indices.length; ++j) {
                const a = index_offset + mesh.indices[j - 2];
                const b = index_offset + mesh.indices[j - 1];
                const c = index_offset + mesh.indices[j];
                const pa = this.builder.get_position(a);
                const pb = this.builder.get_position(b);
                const pc = this.builder.get_position(c);
                const na = this.builder.get_normal(a);
                const nb = this.builder.get_normal(b);
                const nc = this.builder.get_normal(c);

                // Calculate a surface normal and reverse the vertex winding if at least 2 of the vertex normals point in the opposite direction.
                // This hack fixes the winding for most models.
                const normal = pb
                    .clone()
                    .sub(pa)
                    .cross(pc.clone().sub(pa));

                if (clockwise) {
                    normal.negate();
                }

                const opposite_count =
                    (normal.dot(na) < 0 ? 1 : 0) +
                    (normal.dot(nb) < 0 ? 1 : 0) +
                    (normal.dot(nc) < 0 ? 1 : 0);

                if (opposite_count >= 2) {
                    clockwise = !clockwise;
                }

                if (clockwise) {
                    this.builder.add_index(b);
                    this.builder.add_index(a);
                    this.builder.add_index(c);
                } else {
                    this.builder.add_index(a);
                    this.builder.add_index(b);
                    this.builder.add_index(c);
                }

                clockwise = !clockwise;
            }

            if (mesh.material_properties.texture_id != undefined) {
                current_mat_idx = mesh.material_properties.texture_id;
            }

            if (mesh.material_properties.src_alpha != undefined) {
                current_src_alpha = mesh.material_properties.src_alpha;
            }

            if (mesh.material_properties.dst_alpha != undefined) {
                current_dst_alpha = mesh.material_properties.dst_alpha;
            }

            this.builder.add_group(
                start_index_count,
                this.builder.index_count - start_index_count,
                current_mat_idx,
                true,
                current_src_alpha !== 4 || current_dst_alpha !== 5,
            );
        }
    }
}
