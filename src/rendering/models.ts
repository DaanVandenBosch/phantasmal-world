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
        this.object_to_object3d(object, undefined, new Matrix4());

        const geom = new BufferGeometry();

        geom.addAttribute('position', new Float32BufferAttribute(this.positions, 3));
        geom.addAttribute('normal', new Float32BufferAttribute(this.normals, 3));

        if (this.indices.length) {
            geom.setIndex(new Uint16BufferAttribute(this.indices, 1));
        }

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
        console.log(this.bones)

        return mesh;
    }

    private object_to_object3d(
        object: NinjaObject<NinjaModel>,
        parent_bone: Bone | undefined,
        parent_matrix: Matrix4
    ) {
        const {
            no_translate, no_rotate, no_scale, hidden, break_child_trace, zxy_rotation_order, eval_skip
        } = object.evaluation_flags;
        const { position, rotation, scale } = object;

        const euler = new Euler(
            rotation.x, rotation.y, rotation.z, zxy_rotation_order ? 'ZXY' : 'ZYX'
        );
        const matrix = new Matrix4()
            .compose(
                no_translate ? new Vector3() : vec3_to_threejs(position),
                no_rotate ? new Quaternion(0, 0, 0, 1) : new Quaternion().setFromEuler(euler),
                no_scale ? new Vector3(1, 1, 1) : vec3_to_threejs(scale)
            )
            .premultiply(parent_matrix);

        let bone: Bone | undefined;

        if (eval_skip) {
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
                this.object_to_object3d(child, bone, matrix);
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

    // TODO: use indices and don't add duplicate positions/normals.
    private nj_model_to_geometry(model: NjModel, matrix: Matrix4) {
        const positions = this.positions;
        const normals = this.normals;
        const bone_indices = this.bone_indices;
        const bone_weights = this.bone_weights;

        const normal_matrix = new Matrix3().getNormalMatrix(matrix);

        const new_vertices = model.vertices.map(({ position, normal, bone_weight }) => {
            const new_position = vec3_to_threejs(position);
            const new_normal = normal ? vec3_to_threejs(normal) : DEFAULT_NORMAL;

            new_position.applyMatrix4(matrix);
            new_normal.applyMatrix3(normal_matrix);

            return {
                bone_id: this.id,
                position: new_position,
                normal: new_normal,
                bone_weight
            };
        });

        this.vertices.put(new_vertices);

        for (const mesh of model.meshes) {
            for (let i = 2; i < mesh.indices.length; ++i) {
                const a_idx = mesh.indices[i - 2];
                const b_idx = mesh.indices[i - 1];
                const c_idx = mesh.indices[i];
                const a = this.vertices.get(a_idx)[0];
                const b = this.vertices.get(b_idx)[0];
                const c = this.vertices.get(c_idx)[0];

                if (a && b && c) {
                    const a_n = a.normal || DEFAULT_NORMAL;
                    const b_n = b.normal || DEFAULT_NORMAL;
                    const c_n = c.normal || DEFAULT_NORMAL;

                    if (i % 2 === (mesh.clockwise_winding ? 1 : 0)) {
                        positions.push(a.position.x, a.position.y, a.position.z);
                        positions.push(b.position.x, b.position.y, b.position.z);
                        positions.push(c.position.x, c.position.y, c.position.z);
                        normals.push(a_n.x, a_n.y, a_n.z);
                        normals.push(b_n.x, b_n.y, b_n.z);
                        normals.push(c_n.x, c_n.y, c_n.z);
                    } else {
                        positions.push(b.position.x, b.position.y, b.position.z);
                        positions.push(a.position.x, a.position.y, a.position.z);
                        positions.push(c.position.x, c.position.y, c.position.z);
                        normals.push(b_n.x, b_n.y, b_n.z);
                        normals.push(a_n.x, a_n.y, a_n.z);
                        normals.push(c_n.x, c_n.y, c_n.z);
                    }

                    bone_indices.push(a.bone_id, 0, 0, 0);
                    bone_indices.push(b.bone_id, 0, 0, 0);
                    bone_indices.push(c.bone_id, 0, 0, 0);
                    bone_weights.push(1, 0, 0, 0);
                    bone_weights.push(1, 0, 0, 0);
                    bone_weights.push(1, 0, 0, 0);
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
