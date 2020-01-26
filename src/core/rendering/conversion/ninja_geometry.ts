import { is_njcm_model, NjModel, NjObject } from "../../data_formats/parsing/ninja";
import { NjcmModel } from "../../data_formats/parsing/ninja/njcm";
import { XjModel } from "../../data_formats/parsing/ninja/xj";
import { vec3_to_math } from "./index";
import { Mesh } from "../Mesh";
import { SceneNode } from "../Scene";
import { VertexFormat } from "../VertexFormat";
import { EulerOrder, Quat } from "../../math/quaternions";
import { Mat4, Vec2, Vec3 } from "../../math/linear_algebra";

const DEFAULT_NORMAL = new Vec3(0, 1, 0);
const DEFAULT_UV = new Vec2(0, 0);
const NO_TRANSLATION = new Vec3(0, 0, 0);
const NO_ROTATION = new Quat(1, 0, 0, 0);
const NO_SCALE = new Vec3(1, 1, 1);

export function ninja_object_to_node(object: NjObject): SceneNode {
    return new NodeCreator().to_node(object);
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

class NodeCreator {
    private readonly vertices = new VerticesHolder();

    to_node(object: NjObject): SceneNode {
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

        const matrix = Mat4.compose(
            no_translate ? NO_TRANSLATION : position,
            no_rotate
                ? NO_ROTATION
                : Quat.euler_angles(
                      rotation.x,
                      rotation.y,
                      rotation.z,
                      zxy_rotation_order ? EulerOrder.ZXY : EulerOrder.ZYX,
                  ),
            no_scale ? NO_SCALE : scale,
        );

        let mesh: Mesh | undefined;

        if (object.model && !hidden) {
            mesh = this.model_to_mesh(object.model);
        }

        const node = new SceneNode(mesh, matrix);

        if (!break_child_trace) {
            for (const child of object.children) {
                node.add_child(this.to_node(child));
            }
        }

        return node;
    }

    private model_to_mesh(model: NjModel): Mesh {
        if (is_njcm_model(model)) {
            return this.njcm_model_to_mesh(model);
        } else {
            return this.xj_model_to_mesh(model);
        }
    }

    private njcm_model_to_mesh(model: NjcmModel): Mesh {
        const new_vertices = model.vertices.map(vertex => {
            const position = vec3_to_math(vertex.position);
            const normal = vertex.normal ? vec3_to_math(vertex.normal) : DEFAULT_NORMAL;

            return {
                position,
                normal,
                bone_weight: vertex.bone_weight,
                bone_weight_status: vertex.bone_weight_status,
                calc_continue: vertex.calc_continue,
            };
        });

        this.vertices.put(new_vertices);

        const builder = Mesh.builder(VertexFormat.PosNorm);

        for (const mesh of model.meshes) {
            for (let i = 0; i < mesh.vertices.length; ++i) {
                const mesh_vertex = mesh.vertices[i];
                const vertices = this.vertices.get(mesh_vertex.index);

                if (vertices.length) {
                    const vertex = vertices[0];
                    const normal = vertex.normal ?? mesh_vertex.normal ?? DEFAULT_NORMAL;
                    const index = builder.vertex_count;

                    builder.vertex(vertex.position, normal);

                    if (index >= 2) {
                        if (i % 2 === (mesh.clockwise_winding ? 1 : 0)) {
                            builder.triangle(index - 2, index - 1, index);
                        } else {
                            builder.triangle(index - 2, index, index - 1);
                        }
                    }
                }
            }
        }

        return builder.build();
    }

    private xj_model_to_mesh(model: XjModel): Mesh {
        const builder = Mesh.builder(VertexFormat.PosNorm);

        for (const { position, normal } of model.vertices) {
            builder.vertex(vec3_to_math(position), normal ? vec3_to_math(normal) : DEFAULT_NORMAL);
        }

        for (const mesh of model.meshes) {
            let clockwise = false;

            for (let j = 2; j < mesh.indices.length; ++j) {
                const a = mesh.indices[j - 2];
                const b = mesh.indices[j - 1];
                const c = mesh.indices[j];

                if (clockwise) {
                    builder.triangle(b, a, c);
                } else {
                    builder.triangle(a, b, c);
                }

                clockwise = !clockwise;
            }
        }

        return builder.build();
    }
}
