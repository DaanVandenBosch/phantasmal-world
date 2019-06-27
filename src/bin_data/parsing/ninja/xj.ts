import { Matrix3, Matrix4, Vector3 } from 'three';
import { BufferCursor } from '../../BufferCursor';

// TODO:
// - textures
// - colors
// - bump maps
// - animation

export interface XjContext {
    format: 'xj';
    positions: number[];
    normals: number[];
    indices: number[];
}

export function parse_xj_model(cursor: BufferCursor, matrix: Matrix4, context: XjContext): void {
    const { positions, normals, indices } = context;

    cursor.seek(4); // Flags according to QEdit, seemingly always 0.
    const vertex_info_list_offset = cursor.u32();
    cursor.seek(4); // Seems to be the vertexInfoCount, always 1.
    const triangle_strip_list_a_offset = cursor.u32();
    const triangle_strip_a_count = cursor.u32();
    const triangle_strip_list_b_offset = cursor.u32();
    const triangle_strip_b_count = cursor.u32();
    cursor.seek(16); // Bounding sphere position and radius in floats.

    const normal_matrix = new Matrix3().getNormalMatrix(matrix);
    const index_offset = positions.length / 3;

    if (vertex_info_list_offset) {
        cursor.seek_start(vertex_info_list_offset);
        cursor.seek(4); // Possibly the vertex type.
        const vertexList_offset = cursor.u32();
        const vertex_size = cursor.u32();
        const vertex_count = cursor.u32();

        for (let i = 0; i < vertex_count; ++i) {
            cursor.seek_start(vertexList_offset + i * vertex_size);
            const position = new Vector3(
                cursor.f32(),
                cursor.f32(),
                cursor.f32()
            ).applyMatrix4(matrix);
            let normal;

            if (vertex_size === 28 || vertex_size === 32 || vertex_size === 36) {
                normal = new Vector3(
                    cursor.f32(),
                    cursor.f32(),
                    cursor.f32()
                ).applyMatrix3(normal_matrix);
            } else {
                normal = new Vector3(0, 1, 0);
            }

            positions.push(position.x);
            positions.push(position.y);
            positions.push(position.z);
            normals.push(normal.x);
            normals.push(normal.y);
            normals.push(normal.z);
        }
    }

    if (triangle_strip_list_a_offset) {
        parse_triangle_strip_list(
            cursor,
            triangle_strip_list_a_offset,
            triangle_strip_a_count,
            positions,
            normals,
            indices,
            index_offset
        );
    }

    if (triangle_strip_list_b_offset) {
        parse_triangle_strip_list(
            cursor,
            triangle_strip_list_b_offset,
            triangle_strip_b_count,
            positions,
            normals,
            indices,
            index_offset
        );
    }
}

function parse_triangle_strip_list(
    cursor: BufferCursor,
    triangle_strip_list_offset: number,
    triangle_strip_count: number,
    positions: number[],
    normals: number[],
    indices: number[],
    index_offset: number
): void {
    for (let i = 0; i < triangle_strip_count; ++i) {
        cursor.seek_start(triangle_strip_list_offset + i * 20);
        cursor.seek(8); // Skip material information.
        const index_list_offset = cursor.u32();
        const index_count = cursor.u32();
        // Ignoring 4 bytes.

        cursor.seek_start(index_list_offset);
        const strip_indices = cursor.u16_array(index_count);
        let clockwise = true;

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
