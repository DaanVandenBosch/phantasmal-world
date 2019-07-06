import { BufferCursor } from "../../BufferCursor";
import { Vec3 } from "../../Vec3";
import { NjVertex } from "../ninja";

// TODO:
// - textures
// - colors
// - bump maps
// - animation

export type XjModel = {
    type: "xj";
    vertices: NjVertex[];
    meshes: XjTriangleStrip[];
};

export type XjTriangleStrip = {
    indices: number[];
};

export function parse_xj_model(cursor: BufferCursor): XjModel {
    cursor.seek(4); // Flags according to QEdit, seemingly always 0.
    const vertex_info_list_offset = cursor.u32();
    cursor.seek(4); // Seems to be the vertexInfoCount, always 1.
    const triangle_strip_list_a_offset = cursor.u32();
    const triangle_strip_a_count = cursor.u32();
    const triangle_strip_list_b_offset = cursor.u32();
    const triangle_strip_b_count = cursor.u32();
    cursor.seek(16); // Bounding sphere position and radius in floats.

    const model: XjModel = {
        type: "xj",
        vertices: [],
        meshes: [],
    };

    if (vertex_info_list_offset) {
        cursor.seek_start(vertex_info_list_offset);
        cursor.seek(4); // Possibly the vertex type.
        const vertexList_offset = cursor.u32();
        const vertex_size = cursor.u32();
        const vertex_count = cursor.u32();

        for (let i = 0; i < vertex_count; ++i) {
            cursor.seek_start(vertexList_offset + i * vertex_size);
            const position = new Vec3(cursor.f32(), cursor.f32(), cursor.f32());
            let normal: Vec3 | undefined;

            if (vertex_size === 28 || vertex_size === 32 || vertex_size === 36) {
                normal = new Vec3(cursor.f32(), cursor.f32(), cursor.f32());
            }

            model.vertices.push({
                position,
                normal,
                bone_weight: 1.0,
                bone_weight_status: 0,
                calc_continue: true,
            });
        }
    }

    if (triangle_strip_list_a_offset) {
        model.meshes.push(
            ...parse_triangle_strip_list(
                cursor,
                triangle_strip_list_a_offset,
                triangle_strip_a_count
            )
        );
    }

    if (triangle_strip_list_b_offset) {
        model.meshes.push(
            ...parse_triangle_strip_list(
                cursor,
                triangle_strip_list_b_offset,
                triangle_strip_b_count
            )
        );
    }

    return model;
}

function parse_triangle_strip_list(
    cursor: BufferCursor,
    triangle_strip_list_offset: number,
    triangle_strip_count: number
): XjTriangleStrip[] {
    const strips: XjTriangleStrip[] = [];

    for (let i = 0; i < triangle_strip_count; ++i) {
        cursor.seek_start(triangle_strip_list_offset + i * 20);
        cursor.seek(8); // Skip material information.
        const index_list_offset = cursor.u32();
        const index_count = cursor.u32();
        // Ignoring 4 bytes.

        cursor.seek_start(index_list_offset);
        const indices = cursor.u16_array(index_count);

        strips.push({ indices });
    }

    return strips;
}
