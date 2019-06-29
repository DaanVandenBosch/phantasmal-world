import Logger from 'js-logger';
import { BufferCursor } from '../../BufferCursor';
import { Vec3 } from '../../../domain';
import { NinjaVertex } from '.';

const logger = Logger.get('bin_data/parsing/ninja/nj');

// TODO:
// - textures
// - colors
// - bump maps
// - animation
// - deal with vertex information contained in triangle strips

export type NjModel = {
    type: 'nj',
    /**
     * Sparse array of vertices.
     */
    vertices: NinjaVertex[],
    meshes: NjTriangleStrip[],
    // materials: [],
    bounding_sphere_center: Vec3,
    bounding_sphere_radius: number,
}

enum NjChunkType {
    Unknown, Null, Bits, CachePolygonList, DrawPolygonList, Tiny, Material, Vertex, Volume, Strip, End
}

type NjChunk = {
    type: NjChunkType,
    type_id: number,
} & (NjUnknownChunk | NjNullChunk | NjBitsChunk | NjCachePolygonListChunk | NjDrawPolygonListChunk | NjTinyChunk | NjMaterialChunk | NjVertexChunk | NjVolumeChunk | NjStripChunk | NjEndChunk)

type NjUnknownChunk = {
    type: NjChunkType.Unknown,
}

type NjNullChunk = {
    type: NjChunkType.Null,
}

type NjBitsChunk = {
    type: NjChunkType.Bits,
}

type NjCachePolygonListChunk = {
    type: NjChunkType.CachePolygonList,
    cache_index: number,
    offset: number,
}

type NjDrawPolygonListChunk = {
    type: NjChunkType.DrawPolygonList,
    cache_index: number
}

type NjTinyChunk = {
    type: NjChunkType.Tiny,
}

type NjMaterialChunk = {
    type: NjChunkType.Material,
}

type NjVertexChunk = {
    type: NjChunkType.Vertex,
    vertices: NjVertex[]
}

type NjVolumeChunk = {
    type: NjChunkType.Volume,
}

type NjStripChunk = {
    type: NjChunkType.Strip,
    triangle_strips: NjTriangleStrip[]
}

type NjEndChunk = {
    type: NjChunkType.End,
}

type NjVertex = {
    index: number,
    position: Vec3,
    normal?: Vec3,
}

type NjTriangleStrip = {
    clockwise_winding: boolean,
    indices: number[],
}

export function parse_nj_model(cursor: BufferCursor, cached_chunk_offsets: number[]): NjModel {
    const vlist_offset = cursor.u32(); // Vertex list
    const plist_offset = cursor.u32(); // Triangle strip index list
    const bounding_sphere_center = new Vec3(
        cursor.f32(),
        cursor.f32(),
        cursor.f32()
    );
    const bounding_sphere_radius = cursor.f32();
    const vertices: NinjaVertex[] = [];
    const meshes: NjTriangleStrip[] = [];

    if (vlist_offset) {
        cursor.seek_start(vlist_offset);

        for (const chunk of parse_chunks(cursor, cached_chunk_offsets, true)) {
            if (chunk.type === NjChunkType.Vertex) {
                for (const vertex of chunk.vertices) {
                    vertices[vertex.index] = {
                        position: vertex.position,
                        normal: vertex.normal
                    };
                }
            }
        }
    }

    if (plist_offset) {
        cursor.seek_start(plist_offset);

        for (const chunk of parse_chunks(cursor, cached_chunk_offsets, false)) {
            if (chunk.type === NjChunkType.Strip) {
                meshes.push(...chunk.triangle_strips);
            }
        }
    }

    return {
        type: 'nj',
        vertices,
        meshes,
        bounding_sphere_center,
        bounding_sphere_radius
    };
}

// TODO: don't reparse when DrawPolygonList chunk is encountered.
function parse_chunks(
    cursor: BufferCursor,
    cached_chunk_offsets: number[],
    wide_end_chunks: boolean
): NjChunk[] {
    const chunks: NjChunk[] = [];
    let loop = true;

    while (loop) {
        const type_id = cursor.u8();
        const flags = cursor.u8();
        const chunk_start_position = cursor.position;
        let size = 0;

        if (type_id === 0) {
            chunks.push({
                type: NjChunkType.Null,
                type_id
            });
        } else if (1 <= type_id && type_id <= 3) {
            chunks.push({
                type: NjChunkType.Bits,
                type_id
            });
        } else if (type_id === 4) {
            const cache_index = flags;
            const offset = cursor.position;
            chunks.push({
                type: NjChunkType.CachePolygonList,
                type_id,
                cache_index,
                offset
            });
            cached_chunk_offsets[cache_index] = offset;
            loop = false;
        } else if (type_id === 5) {
            const cache_index = flags;
            const cached_offset = cached_chunk_offsets[cache_index];

            if (cached_offset != null) {
                cursor.seek_start(cached_offset);
                chunks.push(
                    ...parse_chunks(cursor, cached_chunk_offsets, wide_end_chunks)
                );
            }

            chunks.push({
                type: NjChunkType.DrawPolygonList,
                type_id,
                cache_index
            });
        } else if (8 <= type_id && type_id <= 9) {
            size = 2;
            chunks.push({
                type: NjChunkType.Tiny,
                type_id
            });
        } else if (17 <= type_id && type_id <= 31) {
            size = 2 + 2 * cursor.u16();
            chunks.push({
                type: NjChunkType.Material,
                type_id
            });
        } else if (32 <= type_id && type_id <= 50) {
            size = 2 + 4 * cursor.u16();
            chunks.push({
                type: NjChunkType.Vertex,
                type_id,
                vertices: parse_vertex_chunk(cursor, type_id, flags)
            });
        } else if (56 <= type_id && type_id <= 58) {
            size = 2 + 2 * cursor.u16();
            chunks.push({
                type: NjChunkType.Volume,
                type_id
            });
        } else if (64 <= type_id && type_id <= 75) {
            size = 2 + 2 * cursor.u16();
            chunks.push({
                type: NjChunkType.Strip,
                type_id,
                triangle_strips: parse_triangle_strip_chunk(cursor, type_id)
            });
        } else if (type_id === 255) {
            size = wide_end_chunks ? 2 : 0;
            chunks.push({
                type: NjChunkType.End,
                type_id
            });
            loop = false;
        } else {
            size = 2 + 2 * cursor.u16();
            chunks.push({
                type: NjChunkType.Unknown,
                type_id
            });
            logger.warn(`Unknown chunk type ${type_id} at offset ${chunk_start_position}.`);
        }

        cursor.seek_start(chunk_start_position + size);
    }

    return chunks;
}

function parse_vertex_chunk(
    cursor: BufferCursor,
    chunk_type_id: number,
    flags: number
): NjVertex[] {
    // There are apparently 4 different sets of vertices, ignore all but set 0.
    if ((flags & 0b11) !== 0) {
        return [];
    }

    const index = cursor.u16();
    const vertex_count = cursor.u16();

    const vertices: NjVertex[] = [];

    for (let i = 0; i < vertex_count; ++i) {
        const vertex: NjVertex = {
            index: index + i,
            position: new Vec3(
                cursor.f32(), // x
                cursor.f32(), // y
                cursor.f32(), // z
            )
        };

        if (chunk_type_id === 32) {
            cursor.seek(4); // Always 1.0
        } else if (chunk_type_id === 33) {
            cursor.seek(4); // Always 1.0
            vertex.normal = new Vec3(
                cursor.f32(), // x
                cursor.f32(), // y
                cursor.f32(), // z
            );
            cursor.seek(4); // Always 0.0
        } else if (35 <= chunk_type_id && chunk_type_id <= 40) {
            if (chunk_type_id === 37) {
                // Ninja flags
                vertex.index = index + cursor.u16();
                cursor.seek(2);
            } else {
                // Skip user flags and material information.
                cursor.seek(4);
            }
        } else if (41 <= chunk_type_id && chunk_type_id <= 47) {
            vertex.normal = new Vec3(
                cursor.f32(), // x
                cursor.f32(), // y
                cursor.f32(), // z
            );

            if (chunk_type_id >= 42) {
                if (chunk_type_id === 44) {
                    // Ninja flags
                    vertex.index = index + cursor.u16();
                    cursor.seek(2);
                } else {
                    // Skip user flags and material information.
                    cursor.seek(4);
                }
            }
        } else if (chunk_type_id >= 48) {
            // Skip 32-bit vertex normal in format: reserved(2)|x(10)|y(10)|z(10)
            cursor.seek(4);

            if (chunk_type_id >= 49) {
                // Skip user flags and material information.
                cursor.seek(4);
            }
        }

        vertices.push(vertex);
    }

    return vertices;
}

function parse_triangle_strip_chunk(
    cursor: BufferCursor,
    chunk_type_id: number
): NjTriangleStrip[] {
    const user_offset_and_strip_count = cursor.u16();
    const user_flags_size = user_offset_and_strip_count >>> 14;
    const strip_count = user_offset_and_strip_count & 0x3FFF;
    let options;

    switch (chunk_type_id) {
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
        default: throw new Error(`Unexpected chunk type ID: ${chunk_type_id}.`);
    }

    const [
        parse_texture_coords,
        parse_color,
        parse_normal,
        parse_texture_coords_hires
    ] = options;

    const strips: NjTriangleStrip[] = [];

    for (let i = 0; i < strip_count; ++i) {
        const winding_flag_and_index_count = cursor.i16();
        const clockwise_winding = winding_flag_and_index_count < 1;
        const index_count = Math.abs(winding_flag_and_index_count);

        const indices = [];

        for (let j = 0; j < index_count; ++j) {
            indices.push(cursor.u16());

            if (parse_texture_coords) {
                cursor.seek(4);
            }

            if (parse_color) {
                cursor.seek(4);
            }

            if (parse_normal) {
                cursor.seek(6);
            }

            if (parse_texture_coords_hires) {
                cursor.seek(8);
            }

            if (j >= 2) {
                cursor.seek(2 * user_flags_size);
            }
        }

        strips.push({ clockwise_winding, indices });
    }

    return strips;
}
