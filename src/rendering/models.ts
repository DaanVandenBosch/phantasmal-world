import { BufferAttribute, BufferGeometry, DoubleSide, Euler, Material, Matrix3, Matrix4, Mesh, MeshLambertMaterial, Object3D, Quaternion, Vector3 } from 'three';
import { vec3_to_threejs } from '.';
import { NinjaModel, NinjaObject } from '../bin_data/parsing/ninja';
import { NjModel } from '../bin_data/parsing/ninja/nj';
import { XjModel } from '../bin_data/parsing/ninja/xj';
import { Vec3 } from '../domain';

const DEFAULT_MATERIAL = new MeshLambertMaterial({
    color: 0xFF00FF,
    side: DoubleSide
});
const DEFAULT_NORMAL = new Vec3(0, 1, 0);

export function ninja_object_to_object3d(
    object: NinjaObject<NinjaModel>,
    material: Material = DEFAULT_MATERIAL
): Object3D {
    return new Object3DCreator(material).create_object_3d(object);
}

/**
 * Generate a single BufferGeometry.
 */
export function ninja_object_to_buffer_geometry(
    object: NinjaObject<NinjaModel>,
    material: Material = DEFAULT_MATERIAL
): BufferGeometry {
    return new Object3DCreator(material).create_buffer_geometry(object);
}

class Object3DCreator {
    private id: number = 0;
    private vertices: { position: Vector3, normal?: Vector3 }[] = [];
    private positions: number[] = [];
    private normals: number[] = [];
    private indices: number[] = [];
    private flat: boolean = false;

    constructor(
        private material: Material
    ) { }

    create_object_3d(object: NinjaObject<NinjaModel>): Object3D {
        return this.object_to_object3d(object, new Matrix4())!;
    }

    create_buffer_geometry(object: NinjaObject<NinjaModel>): BufferGeometry {
        this.flat = true;

        this.object_to_object3d(object, new Matrix4());

        const geom = new BufferGeometry();

        geom.addAttribute('position', new BufferAttribute(new Float32Array(this.positions), 3));
        geom.addAttribute('normal', new BufferAttribute(new Float32Array(this.normals), 3));

        if (this.indices.length) {
            geom.setIndex(new BufferAttribute(new Uint16Array(this.indices), 1));
        }

        // The bounding spheres from the object seem be too small.
        geom.computeBoundingSphere();

        return geom;
    }

    private object_to_object3d(object: NinjaObject<NinjaModel>, parent_matrix: Matrix4): Object3D | undefined {
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

        if (this.flat) {
            if (object.model && !hidden) {
                this.model_to_geometry(object.model, matrix);
            }

            if (!break_child_trace) {
                for (const child of object.children) {
                    this.object_to_object3d(child, matrix);
                }
            }

            return undefined;
        } else {
            let mesh: Object3D;

            if (object.model && !hidden) {
                mesh = new Mesh(
                    this.model_to_geometry(object.model, matrix),
                    this.material
                );
            } else {
                mesh = new Object3D();
            }

            if (!eval_skip) {
                mesh.name = `obj_${this.id++}`;
            }

            mesh.position.set(position.x, position.y, position.z);
            mesh.setRotationFromEuler(euler);
            mesh.scale.set(scale.x, scale.y, scale.z);

            if (!break_child_trace) {
                for (const child of object.children) {
                    mesh.add(this.object_to_object3d(child, matrix)!);
                }
            }

            return mesh;
        }
    }

    private model_to_geometry(model: NinjaModel, matrix: Matrix4): BufferGeometry | undefined {
        if (model.type === 'nj') {
            return this.nj_model_to_geometry(model, matrix);
        } else {
            return this.xj_model_to_geometry(model, matrix);
        }
    }

    // TODO: use indices and don't add duplicate positions/normals.
    private nj_model_to_geometry(model: NjModel, matrix: Matrix4): BufferGeometry | undefined {
        const positions = this.flat ? this.positions : [];
        const normals = this.flat ? this.normals : [];

        const normal_matrix = new Matrix3().getNormalMatrix(matrix);

        const matrix_inverse = new Matrix4().getInverse(matrix);
        const normal_matrix_inverse = new Matrix3().getNormalMatrix(matrix_inverse);

        const new_vertices = model.vertices.map(({ position, normal }) => {
            const new_position = vec3_to_threejs(position).applyMatrix4(matrix);

            const new_normal = normal
                ? vec3_to_threejs(normal).applyMatrix3(normal_matrix)
                : DEFAULT_NORMAL;

            return {
                position: new_position,
                normal: new_normal
            };
        });

        if (this.flat) {
            Object.assign(this.vertices, new_vertices);
        }

        for (const mesh of model.meshes) {
            for (let i = 2; i < mesh.indices.length; ++i) {
                const a_idx = mesh.indices[i - 2];
                const b_idx = mesh.indices[i - 1];
                const c_idx = mesh.indices[i];
                let a;
                let b;
                let c;

                if (this.flat) {
                    a = this.vertices[a_idx];
                    b = this.vertices[b_idx];
                    c = this.vertices[c_idx];
                } else {
                    a = model.vertices[a_idx];
                    b = model.vertices[b_idx];
                    c = model.vertices[c_idx];

                    if (!a && this.vertices[a_idx]) {
                        const { position, normal } = this.vertices[a_idx];
                        a = {
                            position: position.clone().applyMatrix4(matrix_inverse),
                            normal: normal && normal.clone().applyMatrix3(normal_matrix_inverse)
                        };
                    }

                    if (!b && this.vertices[b_idx]) {
                        const { position, normal } = this.vertices[b_idx];
                        b = {
                            position: position.clone().applyMatrix4(matrix_inverse),
                            normal: normal && normal.clone().applyMatrix3(normal_matrix_inverse)
                        };
                    }

                    if (!c && this.vertices[c_idx]) {
                        const { position, normal } = this.vertices[c_idx];
                        c = {
                            position: position.clone().applyMatrix4(matrix_inverse),
                            normal: normal && normal.clone().applyMatrix3(normal_matrix_inverse)
                        };
                    }
                }

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
                }
            }
        }

        if (this.flat) {
            return undefined;
        } else {
            Object.assign(this.vertices, new_vertices);

            const geom = new BufferGeometry();

            geom.addAttribute('position', new BufferAttribute(new Float32Array(positions), 3));
            geom.addAttribute('normal', new BufferAttribute(new Float32Array(normals), 3));
            // The bounding spheres from the object seem be too small.
            geom.computeBoundingSphere();

            return geom;
        }
    }

    private xj_model_to_geometry(model: XjModel, matrix: Matrix4): BufferGeometry | undefined {
        const positions = this.flat ? this.positions : [];
        const normals = this.flat ? this.normals : [];
        const indices = this.flat ? this.indices : [];
        const index_offset = this.flat ? this.positions.length / 3 : 0;
        let clockwise = true;

        const normal_matrix = new Matrix3().getNormalMatrix(matrix);

        for (let { position, normal } of model.vertices) {
            const p = this.flat ? vec3_to_threejs(position).applyMatrix4(matrix) : position;
            positions.push(p.x, p.y, p.z);

            normal = normal || DEFAULT_NORMAL;
            const n = this.flat ? vec3_to_threejs(normal).applyMatrix3(normal_matrix) : normal;
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

        if (this.flat) {
            return undefined;
        } else {
            const geom = new BufferGeometry();

            geom.addAttribute('position', new BufferAttribute(new Float32Array(positions), 3));
            geom.addAttribute('normal', new BufferAttribute(new Float32Array(normals), 3));
            geom.setIndex(new BufferAttribute(new Uint16Array(indices), 1));
            // The bounding spheres from the object seem be too small.
            geom.computeBoundingSphere();

            return geom;
        }
    }
}
