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

export function parseXjModel(cursor: BufferCursor, matrix: Matrix4, context: XjContext): void {
    const { positions, normals, indices } = context;

    cursor.seek(4); // Flags according to QEdit, seemingly always 0.
    const vertexInfoListOffset = cursor.u32();
    cursor.seek(4); // Seems to be the vertexInfoCount, always 1.
    const triangleStripListAOffset = cursor.u32();
    const triangleStripACount = cursor.u32();
    const triangleStripListBOffset = cursor.u32();
    const triangleStripBCount = cursor.u32();
    cursor.seek(16); // Bounding sphere position and radius in floats.

    const normalMatrix = new Matrix3().getNormalMatrix(matrix);
    const indexOffset = positions.length / 3;

    if (vertexInfoListOffset) {
        cursor.seek_start(vertexInfoListOffset);
        cursor.seek(4); // Possibly the vertex type.
        const vertexListOffset = cursor.u32();
        const vertexSize = cursor.u32();
        const vertexCount = cursor.u32();

        for (let i = 0; i < vertexCount; ++i) {
            cursor.seek_start(vertexListOffset + i * vertexSize);
            const position = new Vector3(
                cursor.f32(),
                cursor.f32(),
                cursor.f32()
            ).applyMatrix4(matrix);
            let normal;

            if (vertexSize === 28 || vertexSize === 32 || vertexSize === 36) {
                normal = new Vector3(
                    cursor.f32(),
                    cursor.f32(),
                    cursor.f32()
                ).applyMatrix3(normalMatrix);
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

    if (triangleStripListAOffset) {
        parseTriangleStripList(
            cursor,
            triangleStripListAOffset,
            triangleStripACount,
            positions,
            normals,
            indices,
            indexOffset
        );
    }

    if (triangleStripListBOffset) {
        parseTriangleStripList(
            cursor,
            triangleStripListBOffset,
            triangleStripBCount,
            positions,
            normals,
            indices,
            indexOffset
        );
    }
}

function parseTriangleStripList(
    cursor: BufferCursor,
    triangleStripListOffset: number,
    triangleStripCount: number,
    positions: number[],
    normals: number[],
    indices: number[],
    indexOffset: number
): void {
    for (let i = 0; i < triangleStripCount; ++i) {
        cursor.seek_start(triangleStripListOffset + i * 20);
        cursor.seek(8); // Skip material information.
        const indexListOffset = cursor.u32();
        const indexCount = cursor.u32();
        // Ignoring 4 bytes.

        cursor.seek_start(indexListOffset);
        const stripIndices = cursor.u16_array(indexCount);
        let clockwise = true;

        for (let j = 2; j < stripIndices.length; ++j) {
            const a = indexOffset + stripIndices[j - 2];
            const b = indexOffset + stripIndices[j - 1];
            const c = indexOffset + stripIndices[j];
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

            const oppositeCount =
                (normal.dot(na) < 0 ? 1 : 0) +
                (normal.dot(nb) < 0 ? 1 : 0) +
                (normal.dot(nc) < 0 ? 1 : 0);

            if (oppositeCount >= 2) {
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
