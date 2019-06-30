import { Bone, BufferGeometry, DoubleSide, Euler, Float32BufferAttribute, Material, Matrix3, Matrix4, MeshLambertMaterial, Quaternion, Skeleton, SkinnedMesh, Uint16BufferAttribute, Vector3 } from 'three';
import { vec3_to_threejs } from '.';
import { NinjaModel, NinjaObject } from '../bin_data/parsing/ninja';
import { NjModel } from '../bin_data/parsing/ninja/nj';
import { XjModel } from '../bin_data/parsing/ninja/xj';

const DEFAULT_MATERIAL = new MeshLambertMaterial({
    color: 0xFF00FF,
    side: DoubleSide
});
const DEFAULT_SKINNED_MATERIAL = new MeshLambertMaterial({
    skinning: true,
    color: 0xFF00FF,
    side: DoubleSide
});
const DEFAULT_NORMAL = new Vector3(0, 1, 0);
const NO_TRANSLATION = new Vector3(0, 0, 0);
const NO_ROTATION = new Quaternion(0, 0, 0, 1);
const NO_SCALE = new Vector3(1, 1, 1);

export function ninja_object_to_buffer_geometry(
    object: NinjaObject<NinjaModel>,
    material: Material = DEFAULT_MATERIAL
): BufferGeometry {
    return new Object3DCreator(material).create_buffer_geometry(object);
}

export function ninja_object_to_skinned_mesh(
    object: NinjaObject<NinjaModel>,
    material: Material = DEFAULT_SKINNED_MATERIAL
): SkinnedMesh {
    return new Object3DCreator(material).create_skinned_mesh(object);
}

type Vertex = {
    bone_id: number,
    position: Vector3,
    normal?: Vector3,
    bone_weight: number,
    bone_weight_status: number,
    calc_continue: boolean,
}

class VerticesHolder {
    private vertices_stack: Vertex[][] = [];

    put(vertices: Vertex[]) {
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
    private id: number = 0;
    private vertices = new VerticesHolder();
    private positions: number[] = [];
    private normals: number[] = [];
    private indices: number[] = [];
    private bone_indices: number[] = [];
    private bone_weights: number[] = [];
    private bones: Bone[] = [];

    constructor(
        private material: Material
    ) { }

    create_buffer_geometry(object: NinjaObject<NinjaModel>): BufferGeometry {
        this.object_to_geometry(object, undefined, new Matrix4());

        const geom = new BufferGeometry();

        geom.addAttribute('position', new Float32BufferAttribute(this.positions, 3));
        geom.addAttribute('normal', new Float32BufferAttribute(this.normals, 3));
        geom.setIndex(new Uint16BufferAttribute(this.indices, 1));

        // The bounding spheres from the object seem be too small.
        geom.computeBoundingSphere();

        return geom;
    }

    create_skinned_mesh(object: NinjaObject<NinjaModel>): SkinnedMesh {
        const geom = this.create_buffer_geometry(object);
        geom.addAttribute('skinIndex', new Uint16BufferAttribute(this.bone_indices, 4));
        geom.addAttribute('skinWeight', new Float32BufferAttribute(this.bone_weights, 4));

        const mesh = new SkinnedMesh(geom, this.material);

        const skeleton = new Skeleton(this.bones);
        mesh.add(this.bones[0]);
        mesh.bind(skeleton);

        return mesh;
    }

    private object_to_geometry(
        object: NinjaObject<NinjaModel>,
        parent_bone: Bone | undefined,
        parent_matrix: Matrix4
    ) {
        const {
            no_translate, no_rotate, no_scale, hidden, break_child_trace, zxy_rotation_order, skip
        } = object.evaluation_flags;
        const { position, rotation, scale } = object;

        const euler = new Euler(
            rotation.x, rotation.y, rotation.z, zxy_rotation_order ? 'ZXY' : 'ZYX'
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
            bone.name = (this.id++).toString();

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

        if (!break_child_trace) {
            for (const child of object.children) {
                this.object_to_geometry(child, bone, matrix);
            }
        }
    }

    private model_to_geometry(model: NinjaModel, matrix: Matrix4) {
        if (model.type === 'nj') {
            this.nj_model_to_geometry(model, matrix);
        } else {
            this.xj_model_to_geometry(model, matrix);
        }
    }

    private nj_model_to_geometry(model: NjModel, matrix: Matrix4) {
        const normal_matrix = new Matrix3().getNormalMatrix(matrix);

        const new_vertices = model.vertices.map(vertex => {
            const position = vec3_to_threejs(vertex.position);
            const normal = vertex.normal ? vec3_to_threejs(vertex.normal) : DEFAULT_NORMAL;

            position.applyMatrix4(matrix);
            normal.applyMatrix3(normal_matrix);

            return {
                bone_id: this.id,
                position,
                normal,
                bone_weight: vertex.bone_weight,
                bone_weight_status: vertex.bone_weight_status,
                calc_continue: vertex.calc_continue
            };
        });

        this.vertices.put(new_vertices);

        for (const mesh of model.meshes) {
            for (let i = 0; i < mesh.indices.length; ++i) {
                const vertex_idx = mesh.indices[i];
                const vertices = this.vertices.get(vertex_idx);

                if (vertices.length) {
                    const vertex = vertices[0];
                    const normal = vertex.normal || DEFAULT_NORMAL;
                    const index = this.positions.length / 3;

                    this.positions.push(vertex.position.x, vertex.position.y, vertex.position.z);
                    this.normals.push(normal.x, normal.y, normal.z);

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
        }
    }

    private xj_model_to_geometry(model: XjModel, matrix: Matrix4) {
        const positions = this.positions;
        const normals = this.normals;
        const indices = this.indices;
        const index_offset = this.positions.length / 3;
        let clockwise = true;

        const normal_matrix = new Matrix3().getNormalMatrix(matrix);

        for (let { position, normal } of model.vertices) {
            const p = vec3_to_threejs(position).applyMatrix4(matrix);
            positions.push(p.x, p.y, p.z);

            normal = normal || DEFAULT_NORMAL;
            const n = vec3_to_threejs(normal).applyMatrix3(normal_matrix);
            normals.push(n.x, n.y, n.z);
        }

        for (const mesh of model.meshes) {
            const strip_indices = mesh.indices;

            for (let j = 2; j < strip_indices.length; ++j) {
                const a = index_offset + strip_indices[j - 2];
                const b = index_offset + strip_indices[j - 1];
                const c = index_offset + strip_indices[j];
                const pa = new Vector3(positions[3 * a], positions[3 * a + 1], positions[3 * a + 2]);
                const pb = new Vector3(positions[3 * b], positions[3 * b + 1], positions[3 * b + 2]);
                const pc = new Vector3(positions[3 * c], positions[3 * c + 1], positions[3 * c + 2]);
                const na = new Vector3(normals[3 * a], normals[3 * a + 1], normals[3 * a + 2]);
                const nb = new Vector3(normals[3 * a], normals[3 * a + 1], normals[3 * a + 2]);
                const nc = new Vector3(normals[3 * a], normals[3 * a + 1], normals[3 * a + 2]);

                // Calculate a surface normal and reverse the vertex winding if at least 2 of the vertex normals point in the opposite direction.
                // This hack fixes the winding for most models.
                const normal = pb.clone().sub(pa).cross(pc.clone().sub(pa));

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
                    indices.push(b);
                    indices.push(a);
                    indices.push(c);
                } else {
                    indices.push(a);
                    indices.push(b);
                    indices.push(c);
                }

                clockwise = !clockwise;

                // The following switch statement fixes model 180.xj (zanba).
                // switch (j) {
                //     case 17:
                //     case 52:
                //     case 70:
                //     case 92:
                //     case 97:
                //     case 126:
                //     case 140:
                //     case 148:
                //     case 187:
                //     case 200:
                //         console.warn(`swapping winding at: ${j}, (${a}, ${b}, ${c})`);
                //         break;
                //     default:
                //         ccw = !ccw;
                //         break;
                // }
            }
        }
    }
}
