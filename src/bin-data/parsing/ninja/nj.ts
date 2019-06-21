import { Matrix3, Matrix4, Vector3 } from 'three';
import { ArrayBufferCursor } from '../../ArrayBufferCursor';
import Logger from 'js-logger';

const logger = Logger.get('bin-data/parsing/ninja/nj');

// TODO:
// - deal with multiple NJCM chunks
// - deal with other types of chunks
// - textures
// - colors
// - bump maps
// - animation
// - deal with vertex information contained in triangle strips

export interface NjContext {
    format: 'nj';
    positions: number[];
    normals: number[];
    cachedChunkOffsets: number[];
    vertices: { position: Vector3, normal: Vector3 }[];
}

interface Node {
    vertices: { position: Vector3, normal: Vector3 }[];
    indices: number[];
    parent?: Node;
    children: Node[];
}

interface ChunkVertex {
    index: number;
    position: [number, number, number];
    normal?: [number, number, number];
}

interface ChunkTriangleStrip {
    clockwiseWinding: boolean;
    indices: number[];
}

export function parseNjModel(cursor: ArrayBufferCursor, matrix: Matrix4, context: NjContext): void {
    const { positions, normals, cachedChunkOffsets, vertices } = context;

    const vlistOffset = cursor.u32(); // Vertex list
    const plistOffset = cursor.u32(); // Triangle strip index list

    const normalMatrix = new Matrix3().getNormalMatrix(matrix);

    if (vlistOffset) {
        cursor.seekStart(vlistOffset);

        for (const chunk of parseChunks(cursor, cachedChunkOffsets, true)) {
            if (chunk.chunkType === 'VERTEX') {
                const chunkVertices: ChunkVertex[] = chunk.data;

                for (const vertex of chunkVertices) {
                    const position = new Vector3(...vertex.position).applyMatrix4(matrix);
                    const normal = vertex.normal ? new Vector3(...vertex.normal).applyMatrix3(normalMatrix) : new Vector3(0, 1, 0);
                    vertices[vertex.index] = { position, normal };
                }
            }
        }
    }

    if (plistOffset) {
        cursor.seekStart(plistOffset);

        for (const chunk of parseChunks(cursor, cachedChunkOffsets, false)) {
            if (chunk.chunkType === 'STRIP') {
                for (const { clockwiseWinding, indices: stripIndices } of chunk.data) {
                    for (let j = 2; j < stripIndices.length; ++j) {
                        const a = vertices[stripIndices[j - 2]];
                        const b = vertices[stripIndices[j - 1]];
                        const c = vertices[stripIndices[j]];

                        if (a && b && c) {
                            if (j % 2 === (clockwiseWinding ? 1 : 0)) {
                                positions.splice(positions.length, 0, a.position.x, a.position.y, a.position.z);
                                positions.splice(positions.length, 0, b.position.x, b.position.y, b.position.z);
                                positions.splice(positions.length, 0, c.position.x, c.position.y, c.position.z);
                                normals.splice(normals.length, 0, a.normal.x, a.normal.y, a.normal.z);
                                normals.splice(normals.length, 0, b.normal.x, b.normal.y, b.normal.z);
                                normals.splice(normals.length, 0, c.normal.x, c.normal.y, c.normal.z);
                            } else {
                                positions.splice(positions.length, 0, b.position.x, b.position.y, b.position.z);
                                positions.splice(positions.length, 0, a.position.x, a.position.y, a.position.z);
                                positions.splice(positions.length, 0, c.position.x, c.position.y, c.position.z);
                                normals.splice(normals.length, 0, b.normal.x, b.normal.y, b.normal.z);
                                normals.splice(normals.length, 0, a.normal.x, a.normal.y, a.normal.z);
                                normals.splice(normals.length, 0, c.normal.x, c.normal.y, c.normal.z);
                            }
                        }
                    }
                }
            }
        }
    }
}

function parseChunks(cursor: ArrayBufferCursor, cachedChunkOffsets: number[], wideEndChunks: boolean): Array<{
    chunkType: string,
    chunkSubType: string | null,
    chunkTypeId: number,
    data: any
}> {
    const chunks = [];
    let loop = true;

    while (loop) {
        const chunkTypeId = cursor.u8();
        const flags = cursor.u8();
        const chunkStartPosition = cursor.position;
        let chunkType = 'UNKOWN';
        let chunkSubType = null;
        let data = null;
        let size = 0;

        if (chunkTypeId === 0) {
            chunkType = 'NULL';
        } else if (1 <= chunkTypeId && chunkTypeId <= 5) {
            chunkType = 'BITS';

            if (chunkTypeId === 4) {
                chunkSubType = 'CACHE_POLYGON_LIST';
                data = {
                    storeIndex: flags,
                    offset: cursor.position
                };
                cachedChunkOffsets[data.storeIndex] = data.offset;
                loop = false;
            } else if (chunkTypeId === 5) {
                chunkSubType = 'DRAW_POLYGON_LIST';
                data = {
                    storeIndex: flags
                };
                cursor.seekStart(cachedChunkOffsets[data.storeIndex]);
                chunks.splice(chunks.length, 0, ...parseChunks(cursor, cachedChunkOffsets, wideEndChunks));
            }
        } else if (8 <= chunkTypeId && chunkTypeId <= 9) {
            chunkType = 'TINY';
            size = 2;
        } else if (17 <= chunkTypeId && chunkTypeId <= 31) {
            chunkType = 'MATERIAL';
            size = 2 + 2 * cursor.u16();
        } else if (32 <= chunkTypeId && chunkTypeId <= 50) {
            chunkType = 'VERTEX';
            size = 2 + 4 * cursor.u16();
            data = parseChunkVertex(cursor, chunkTypeId, flags);
        } else if (56 <= chunkTypeId && chunkTypeId <= 58) {
            chunkType = 'VOLUME';
            size = 2 + 2 * cursor.u16();
        } else if (64 <= chunkTypeId && chunkTypeId <= 75) {
            chunkType = 'STRIP';
            size = 2 + 2 * cursor.u16();
            data = parseChunkTriangleStrip(cursor, chunkTypeId);
        } else if (chunkTypeId === 255) {
            chunkType = 'END';
            size = wideEndChunks ? 2 : 0;
            loop = false;
        } else {
            // Ignore unknown chunks.
            logger.warn(`Unknown chunk type: ${chunkTypeId}.`);
            size = 2 + 2 * cursor.u16();
        }

        cursor.seekStart(chunkStartPosition + size);

        chunks.push({
            chunkType,
            chunkSubType,
            chunkTypeId,
            data
        });
    }

    return chunks;
}

function parseChunkVertex(cursor: ArrayBufferCursor, chunkTypeId: number, flags: number): ChunkVertex[] {
    // There are apparently 4 different sets of vertices, ignore all but set 0.
    if ((flags & 0b11) !== 0) {
        return [];
    }

    const index = cursor.u16();
    const vertexCount = cursor.u16();

    const vertices: ChunkVertex[] = [];

    for (let i = 0; i < vertexCount; ++i) {
        const vertex: ChunkVertex = {
            index: index + i,
            position: [
                cursor.f32(), // x
                cursor.f32(), // y
                cursor.f32(), // z
            ]
        };

        if (chunkTypeId === 32) {
            cursor.seek(4); // Always 1.0
        } else if (chunkTypeId === 33) {
            cursor.seek(4); // Always 1.0
            vertex.normal = [
                cursor.f32(), // x
                cursor.f32(), // y
                cursor.f32(), // z
            ];
            cursor.seek(4); // Always 0.0
        } else if (35 <= chunkTypeId && chunkTypeId <= 40) {
            if (chunkTypeId === 37) {
                // Ninja flags
                vertex.index = index + cursor.u16();
                cursor.seek(2);
            } else {
                // Skip user flags and material information.
                cursor.seek(4);
            }
        } else if (41 <= chunkTypeId && chunkTypeId <= 47) {
            vertex.normal = [
                cursor.f32(), // x
                cursor.f32(), // y
                cursor.f32(), // z
            ];

            if (chunkTypeId >= 42) {
                if (chunkTypeId === 44) {
                    // Ninja flags
                    vertex.index = index + cursor.u16();
                    cursor.seek(2);
                } else {
                    // Skip user flags and material information.
                    cursor.seek(4);
                }
            }
        } else if (chunkTypeId >= 48) {
            // Skip 32-bit vertex normal in format: reserved(2)|x(10)|y(10)|z(10)
            cursor.seek(4);

            if (chunkTypeId >= 49) {
                // Skip user flags and material information.
                cursor.seek(4);
            }
        }

        vertices.push(vertex);
    }

    return vertices;
}

function parseChunkTriangleStrip(cursor: ArrayBufferCursor, chunkTypeId: number): ChunkTriangleStrip[] {
    const userOffsetAndStripCount = cursor.u16();
    const userFlagsSize = userOffsetAndStripCount >>> 14;
    const stripCount = userOffsetAndStripCount & 0x3FFF;
    let options;

    switch (chunkTypeId) {
        case 64: options = [false, false, false, false]; break;
        case 65: options = [true, false, false, false]; break;
        case 66: options = [true, false, false, false]; break;
        case 67: options = [false, false, true, false]; break;
        case 68: options = [true, false, true, false]; break;
        case 69: options = [true, false, true, false]; break;
        case 70: options = [false, true, false, false]; break;
        case 71: options = [true, true, false, false]; break;
        case 72: options = [true, true, false, false]; break;
        case 73: options = [false, false, false, false]; break;
        case 74: options = [true, false, false, true]; break;
        case 75: options = [true, false, false, true]; break;
        default: throw new Error(`Unexpected chunk type ID: ${chunkTypeId}.`);
    }

    const [
        parseTextureCoords,
        parseColor,
        parseNormal,
        parseTextureCoordsHires
    ] = options;

    const strips = [];

    for (let i = 0; i < stripCount; ++i) {
        const windingFlagAndIndexCount = cursor.i16();
        const clockwiseWinding = windingFlagAndIndexCount < 1;
        const indexCount = Math.abs(windingFlagAndIndexCount);

        const indices = [];

        for (let j = 0; j < indexCount; ++j) {
            indices.push(cursor.u16());

            if (parseTextureCoords) {
                cursor.seek(4);
            }

            if (parseColor) {
                cursor.seek(4);
            }

            if (parseNormal) {
                cursor.seek(6);
            }

            if (parseTextureCoordsHires) {
                cursor.seek(8);
            }

            if (j >= 2) {
                cursor.seek(2 * userFlagsSize);
            }
        }

        strips.push({ clockwiseWinding, indices });
    }

    return strips;
}
