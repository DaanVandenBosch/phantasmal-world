import { Matrix3, Matrix4, Vector3 } from "three";
import { vec3_to_threejs } from ".";
import { XjModel } from "../data_formats/parsing/ninja/xj";

const DEFAULT_NORMAL = new Vector3(0, 1, 0);

export function xj_model_to_geometry(
    model: XjModel,
    matrix: Matrix4,
    positions: number[],
    normals: number[],
    indices: number[]
): void {
    const index_offset = positions.length / 3;
    const normal_matrix = new Matrix3().getNormalMatrix(matrix);

    for (let { position, normal } of model.vertices) {
        const p = vec3_to_threejs(position).applyMatrix4(matrix);
        positions.push(p.x, p.y, p.z);

        const local_n = normal ? vec3_to_threejs(normal) : DEFAULT_NORMAL;
        const n = local_n.applyMatrix3(normal_matrix);
        normals.push(n.x, n.y, n.z);
    }

    for (const mesh of model.meshes) {
        let clockwise = true;

        for (let j = 2; j < mesh.indices.length; ++j) {
            const a = index_offset + mesh.indices[j - 2];
            const b = index_offset + mesh.indices[j - 1];
            const c = index_offset + mesh.indices[j];
            const pa = new Vector3(positions[3 * a], positions[3 * a + 1], positions[3 * a + 2]);
            const pb = new Vector3(positions[3 * b], positions[3 * b + 1], positions[3 * b + 2]);
            const pc = new Vector3(positions[3 * c], positions[3 * c + 1], positions[3 * c + 2]);
            const na = new Vector3(normals[3 * a], normals[3 * a + 1], normals[3 * a + 2]);
            const nb = new Vector3(normals[3 * a], normals[3 * a + 1], normals[3 * a + 2]);
            const nc = new Vector3(normals[3 * a], normals[3 * a + 1], normals[3 * a + 2]);

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
                indices.push(b);
                indices.push(a);
                indices.push(c);
            } else {
                indices.push(a);
                indices.push(b);
                indices.push(c);
            }

            clockwise = !clockwise;
        }
    }
}
