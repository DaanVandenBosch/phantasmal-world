import { is_njcm_model, NjModel, NjObject } from "../../data_formats/parsing/ninja";
import { NjcmModel } from "../../data_formats/parsing/ninja/njcm";
import { XjModel } from "../../data_formats/parsing/ninja/xj";
import { vec3_to_math } from "./index";
import { Mesh } from "../Mesh";
import { VertexFormat } from "../VertexFormat";
import { EulerOrder, Quat } from "../../math/quaternions";
import {
    mat3_vec3_multiply,
    Mat4,
    mat4_product,
    mat4_vec3_multiply,
    Vec2,
    Vec3,
} from "../../math/linear_algebra";

const DEFAULT_NORMAL = new Vec3(0, 1, 0);
const DEFAULT_UV = new Vec2(0, 0);
const NO_TRANSLATION = new Vec3(0, 0, 0);
const NO_ROTATION = new Quat(1, 0, 0, 0);
const NO_SCALE = new Vec3(1, 1, 1);

export function ninja_object_to_mesh(object: NjObject): Mesh {
    return new MeshCreator().to_mesh(object);
}

type Vertex = {
    position: Vec3;
    normal?: Vec3;
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

class MeshCreator {
    private readonly vertices = new VerticesHolder();
    private readonly builder = Mesh.builder(VertexFormat.PosNorm);

    to_mesh(object: NjObject): Mesh {
        this.object_to_mesh(object, Mat4.identity());
        return this.builder.build();
    }

    private object_to_mesh(object: NjObject, parent_matrix: Mat4): void {
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

        const matrix = mat4_product(
            parent_matrix,
            Mat4.compose(
                no_translate ? NO_TRANSLATION : vec3_to_math(position),
                no_rotate
                    ? NO_ROTATION
                    : Quat.euler_angles(
                          rotation.x,
                          rotation.y,
                          rotation.z,
                          zxy_rotation_order ? EulerOrder.ZXY : EulerOrder.ZYX,
                      ),
                no_scale ? NO_SCALE : vec3_to_math(scale),
            ),
        );

        if (object.model && !hidden) {
            this.model_to_mesh(object.model, matrix);
        }

        if (!break_child_trace) {
            for (const child of object.children) {
                this.object_to_mesh(child, matrix);
            }
        }
    }

    private model_to_mesh(model: NjModel, matrix: Mat4): void {
        if (is_njcm_model(model)) {
            this.njcm_model_to_mesh(model, matrix);
        } else {
            this.xj_model_to_mesh(model, matrix);
        }
    }

    private njcm_model_to_mesh(model: NjcmModel, matrix: Mat4): void {
        const normal_matrix = matrix.normal_mat3();

        const new_vertices = model.vertices.map(vertex => {
            const position = vec3_to_math(vertex.position);
            mat4_vec3_multiply(matrix, position);

            let normal: Vec3 | undefined = undefined;

            if (vertex.normal) {
                normal = vec3_to_math(vertex.normal);
                mat3_vec3_multiply(normal_matrix, normal);
            }

            return {
                position,
                normal,
                bone_weight: vertex.bone_weight,
                bone_weight_status: vertex.bone_weight_status,
                calc_continue: vertex.calc_continue,
            };
        });

        this.vertices.put(new_vertices);

        for (const mesh of model.meshes) {
            for (let i = 0; i < mesh.vertices.length; ++i) {
                const mesh_vertex = mesh.vertices[i];
                const vertices = this.vertices.get(mesh_vertex.index);

                if (vertices.length) {
                    const vertex = vertices[0];
                    const normal =
                        vertex.normal ??
                        (mesh_vertex.normal ? vec3_to_math(mesh_vertex.normal) : DEFAULT_NORMAL);
                    const index = this.builder.vertex_count;

                    this.builder.vertex(vertex.position, normal);

                    if (index >= 2) {
                        if (i % 2 === (mesh.clockwise_winding ? 1 : 0)) {
                            this.builder.triangle(index - 2, index - 1, index);
                        } else {
                            this.builder.triangle(index - 2, index, index - 1);
                        }
                    }
                }
            }
        }
    }

    private xj_model_to_mesh(model: XjModel, matrix: Mat4): void {
        const index_offset = this.builder.vertex_count;
        const normal_matrix = matrix.normal_mat3();

        for (const { position, normal } of model.vertices) {
            const p = vec3_to_math(position);
            mat4_vec3_multiply(matrix, p);

            const n = normal ? vec3_to_math(normal) : new Vec3(0, 1, 0);
            mat3_vec3_multiply(normal_matrix, n);

            this.builder.vertex(p, n);
        }

        for (const mesh of model.meshes) {
            let clockwise = false;

            for (let j = 2; j < mesh.indices.length; ++j) {
                const a = index_offset + mesh.indices[j - 2];
                const b = index_offset + mesh.indices[j - 1];
                const c = index_offset + mesh.indices[j];

                if (clockwise) {
                    this.builder.triangle(b, a, c);
                } else {
                    this.builder.triangle(a, b, c);
                }

                clockwise = !clockwise;
            }
        }
    }
}
