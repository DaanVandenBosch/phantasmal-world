import Logger from "js-logger";
import { Cursor } from "../../cursor/Cursor";
import { Vec2, Vec3 } from "../../vector";

const logger = Logger.get("data_formats/parsing/ninja/njcm");

// TODO:
// - colors
// - bump maps

export type NjcmModel = {
    type: "njcm";
    /**
     * Sparse array of vertices.
     */
    vertices: NjcmVertex[];
    meshes: NjcmTriangleStrip[];
    collision_sphere_center: Vec3;
    collision_sphere_radius: number;
};

export type NjcmVertex = {
    position: Vec3;
    normal?: Vec3;
    bone_weight: number;
    bone_weight_status: number;
    calc_continue: boolean;
};

enum NjcmChunkType {
    Unknown,
    Null,
    Bits,
    CachePolygonList,
    DrawPolygonList,
    Tiny,
    Material,
    Vertex,
    Volume,
    Strip,
    End,
}

type NjcmChunk = {
    type: NjcmChunkType;
    type_id: number;
} & (
    | NjcmUnknownChunk
    | NjcmNullChunk
    | NjcmBitsChunk
    | NjcmCachePolygonListChunk
    | NjcmDrawPolygonListChunk
    | NjcmTinyChunk
    | NjcmMaterialChunk
    | NjcmVertexChunk
    | NjcmVolumeChunk
    | NjcmStripChunk
    | NjcmEndChunk);

type NjcmUnknownChunk = {
    type: NjcmChunkType.Unknown;
};

type NjcmNullChunk = {
    type: NjcmChunkType.Null;
};

type NjcmBitsChunk = {
    type: NjcmChunkType.Bits;
};

type NjcmCachePolygonListChunk = {
    type: NjcmChunkType.CachePolygonList;
    cache_index: number;
    offset: number;
};

type NjcmDrawPolygonListChunk = {
    type: NjcmChunkType.DrawPolygonList;
    cache_index: number;
};

type NjcmTinyChunk = {
    type: NjcmChunkType.Tiny;
    flip_u: boolean;
    flip_v: boolean;
    clamp_u: boolean;
    clamp_v: boolean;
    mipmap_d_adjust: number;
    filter_mode: number;
    super_sample: boolean;
    texture_id: number;
};

type NjcmMaterialChunk = {
    type: NjcmChunkType.Material;
};

type NjcmVertexChunk = {
    type: NjcmChunkType.Vertex;
    vertices: NjcmChunkVertex[];
};

type NjcmVolumeChunk = {
    type: NjcmChunkType.Volume;
};

type NjcmStripChunk = {
    type: NjcmChunkType.Strip;
    triangle_strips: NjcmTriangleStrip[];
};

type NjcmEndChunk = {
    type: NjcmChunkType.End;
};

type NjcmChunkVertex = {
    index: number;
    position: Vec3;
    normal?: Vec3;
    bone_weight: number;
    bone_weight_status: number;
    calc_continue: boolean;
};

type NjcmTriangleStrip = {
    ignore_light: boolean;
    ignore_specular: boolean;
    ignore_ambient: boolean;
    use_alpha: boolean;
    double_side: boolean;
    flat_shading: boolean;
    environment_mapping: boolean;
    clockwise_winding: boolean;
    has_tex_coords: boolean;
    has_normal: boolean;
    texture_id?: number;
    vertices: NjcmMeshVertex[];
};

type NjcmMeshVertex = {
    index: number;
    normal?: Vec3;
    tex_coords?: Vec2;
};

export function parse_njcm_model(cursor: Cursor, cached_chunk_offsets: number[]): NjcmModel {
    const vlist_offset = cursor.u32(); // Vertex list
    const plist_offset = cursor.u32(); // Triangle strip index list
    const bounding_sphere_center = cursor.vec3_f32();
    const bounding_sphere_radius = cursor.f32();
    const vertices: NjcmVertex[] = [];
    const meshes: NjcmTriangleStrip[] = [];

    if (vlist_offset) {
        cursor.seek_start(vlist_offset);

        for (const chunk of parse_chunks(cursor, cached_chunk_offsets, true)) {
            if (chunk.type === NjcmChunkType.Vertex) {
                for (const vertex of chunk.vertices) {
                    vertices[vertex.index] = {
                        position: vertex.position,
                        normal: vertex.normal,
                        bone_weight: vertex.bone_weight,
                        bone_weight_status: vertex.bone_weight_status,
                        calc_continue: vertex.calc_continue,
                    };
                }
            }
        }
    }

    if (plist_offset) {
        cursor.seek_start(plist_offset);

        let texture_id: number | undefined;

        for (const chunk of parse_chunks(cursor, cached_chunk_offsets, false)) {
            if (chunk.type === NjcmChunkType.Tiny) {
                texture_id = chunk.texture_id;
            } else if (chunk.type === NjcmChunkType.Strip) {
                for (const strip of chunk.triangle_strips) {
                    strip.texture_id = texture_id;
                }

                meshes.push(...chunk.triangle_strips);
            }
        }
    }

    return {
        type: "njcm",
        vertices,
        meshes,
        collision_sphere_center: bounding_sphere_center,
        collision_sphere_radius: bounding_sphere_radius,
    };
}

// TODO: don't reparse when DrawPolygonList chunk is encountered.
function parse_chunks(
    cursor: Cursor,
    cached_chunk_offsets: number[],
    wide_end_chunks: boolean,
): NjcmChunk[] {
    const chunks: NjcmChunk[] = [];
    let loop = true;

    while (loop) {
        const type_id = cursor.u8();
        const flags = cursor.u8();
        const chunk_start_position = cursor.position;
        let size = 0;

        if (type_id === 0) {
            chunks.push({
                type: NjcmChunkType.Null,
                type_id,
            });
        } else if (1 <= type_id && type_id <= 3) {
            chunks.push({
                type: NjcmChunkType.Bits,
                type_id,
            });
        } else if (type_id === 4) {
            const cache_index = flags;
            const offset = cursor.position;
            chunks.push({
                type: NjcmChunkType.CachePolygonList,
                type_id,
                cache_index,
                offset,
            });
            cached_chunk_offsets[cache_index] = offset;
            loop = false;
        } else if (type_id === 5) {
            const cache_index = flags;
            const cached_offset = cached_chunk_offsets[cache_index];

            if (cached_offset != null) {
                cursor.seek_start(cached_offset);
                chunks.push(...parse_chunks(cursor, cached_chunk_offsets, wide_end_chunks));
            }

            chunks.push({
                type: NjcmChunkType.DrawPolygonList,
                type_id,
                cache_index,
            });
        } else if (8 <= type_id && type_id <= 9) {
            size = 2;
            const texture_bits_and_id = cursor.u16();
            chunks.push({
                type: NjcmChunkType.Tiny,
                type_id,
                flip_u: (type_id & 0x80) !== 0,
                flip_v: (type_id & 0x40) !== 0,
                clamp_u: (type_id & 0x20) !== 0,
                clamp_v: (type_id & 0x10) !== 0,
                mipmap_d_adjust: type_id & 0b1111,
                filter_mode: texture_bits_and_id >>> 14,
                super_sample: (texture_bits_and_id & 0x40) !== 0,
                texture_id: texture_bits_and_id & 0x1fff,
            });
        } else if (17 <= type_id && type_id <= 31) {
            size = 2 + 2 * cursor.u16();
            chunks.push({
                type: NjcmChunkType.Material,
                type_id,
            });
        } else if (32 <= type_id && type_id <= 50) {
            size = 2 + 4 * cursor.u16();
            chunks.push({
                type: NjcmChunkType.Vertex,
                type_id,
                vertices: parse_vertex_chunk(cursor, type_id, flags),
            });
        } else if (56 <= type_id && type_id <= 58) {
            size = 2 + 2 * cursor.u16();
            chunks.push({
                type: NjcmChunkType.Volume,
                type_id,
            });
        } else if (64 <= type_id && type_id <= 75) {
            size = 2 + 2 * cursor.u16();
            chunks.push({
                type: NjcmChunkType.Strip,
                type_id,
                triangle_strips: parse_triangle_strip_chunk(cursor, type_id, flags),
            });
        } else if (type_id === 255) {
            size = wide_end_chunks ? 2 : 0;
            chunks.push({
                type: NjcmChunkType.End,
                type_id,
            });
            loop = false;
        } else {
            size = 2 + 2 * cursor.u16();
            chunks.push({
                type: NjcmChunkType.Unknown,
                type_id,
            });
            logger.warn(`Unknown chunk type ${type_id} at offset ${chunk_start_position}.`);
        }

        cursor.seek_start(chunk_start_position + size);
    }

    return chunks;
}

function parse_vertex_chunk(
    cursor: Cursor,
    chunk_type_id: number,
    flags: number,
): NjcmChunkVertex[] {
    if (chunk_type_id < 32 || chunk_type_id > 50) {
        logger.warn(`Unknown vertex chunk type ${chunk_type_id}.`);
        return [];
    }

    const bone_weight_status = flags & 0b11;
    const calc_continue = (flags & 0x80) !== 0;

    const index = cursor.u16();
    const vertex_count = cursor.u16();

    const vertices: NjcmChunkVertex[] = [];

    for (let i = 0; i < vertex_count; ++i) {
        const vertex: NjcmChunkVertex = {
            index: index + i,
            position: cursor.vec3_f32(),
            bone_weight: 1,
            bone_weight_status,
            calc_continue,
        };

        if (chunk_type_id === 32) {
            // NJD_CV_SH
            cursor.seek(4); // Always 1.0
        } else if (chunk_type_id === 33) {
            // NJD_CV_VN_SH
            cursor.seek(4); // Always 1.0
            vertex.normal = cursor.vec3_f32();
            cursor.seek(4); // Always 0.0
        } else if (35 <= chunk_type_id && chunk_type_id <= 40) {
            if (chunk_type_id === 37) {
                // NJD_CV_NF
                // NinjaFlags32
                vertex.index = index + cursor.u16();
                vertex.bone_weight = cursor.u16() / 255;
            } else {
                // Skip user flags and material information.
                cursor.seek(4);
            }
        } else if (41 <= chunk_type_id && chunk_type_id <= 47) {
            vertex.normal = cursor.vec3_f32();

            if (chunk_type_id >= 42) {
                if (chunk_type_id === 44) {
                    // NJD_CV_VN_NF
                    // NinjaFlags32
                    vertex.index = index + cursor.u16();
                    vertex.bone_weight = cursor.u16() / 255;
                } else {
                    // Skip user flags and material information.
                    cursor.seek(4);
                }
            }
        } else if (48 <= chunk_type_id && chunk_type_id <= 50) {
            // 32-Bit vertex normal in format: reserved(2)|x(10)|y(10)|z(10)
            const normal = cursor.u32();
            vertex.normal = new Vec3(
                ((normal >> 20) & 0x3ff) / 0x3ff,
                ((normal >> 10) & 0x3ff) / 0x3ff,
                (normal & 0x3ff) / 0x3ff,
            );

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
    cursor: Cursor,
    chunk_type_id: number,
    flags: number,
): NjcmTriangleStrip[] {
    const render_flags = {
        ignore_light: (flags & 0b1) !== 0,
        ignore_specular: (flags & 0b10) !== 0,
        ignore_ambient: (flags & 0b100) !== 0,
        use_alpha: (flags & 0b1000) !== 0,
        double_side: (flags & 0b10000) !== 0,
        flat_shading: (flags & 0b100000) !== 0,
        environment_mapping: (flags & 0b1000000) !== 0,
    };
    const user_offset_and_strip_count = cursor.u16();
    const user_flags_size = user_offset_and_strip_count >>> 14;
    const strip_count = user_offset_and_strip_count & 0x3fff;

    let has_tex_coords = false;
    let has_color = false;
    let has_normal = false;
    let has_double_tex_coords = false;

    switch (chunk_type_id) {
        case 64:
            break;
        case 65:
        case 66:
            has_tex_coords = true;
            break;
        case 67:
            has_normal = true;
            break;
        case 68:
        case 69:
            has_tex_coords = true;
            has_normal = true;
            break;
        case 70:
            has_color = true;
            break;
        case 71:
        case 72:
            has_tex_coords = true;
            has_color = true;
            break;
        case 73:
            break;
        case 74:
        case 75:
            has_double_tex_coords = true;
            break;
        default:
            throw new Error(`Unexpected chunk type ID: ${chunk_type_id}.`);
    }

    const strips: NjcmTriangleStrip[] = [];

    for (let i = 0; i < strip_count; ++i) {
        const winding_flag_and_index_count = cursor.i16();
        const clockwise_winding = winding_flag_and_index_count < 1;
        const index_count = Math.abs(winding_flag_and_index_count);

        const vertices: NjcmMeshVertex[] = [];

        for (let j = 0; j < index_count; ++j) {
            const vertex: NjcmMeshVertex = {
                index: cursor.u16(),
            };
            vertices.push(vertex);

            if (has_tex_coords) {
                vertex.tex_coords = new Vec2(cursor.u16() / 255, cursor.u16() / 255);
            }

            // Ignore ARGB8888 color.
            if (has_color) {
                cursor.seek(4);
            }

            if (has_normal) {
                vertex.normal = new Vec3(
                    cursor.u16() / 255,
                    cursor.u16() / 255,
                    cursor.u16() / 255,
                );
            }

            // Ignore double texture coordinates (Ua, Vb, Ua, Vb).
            if (has_double_tex_coords) {
                cursor.seek(8);
            }

            // User flags start at the third vertex because they are per-triangle.
            if (j >= 2) {
                cursor.seek(2 * user_flags_size);
            }
        }

        strips.push({
            ...render_flags,
            clockwise_winding,
            has_tex_coords,
            has_normal,
            vertices,
        });
    }

    return strips;
}
