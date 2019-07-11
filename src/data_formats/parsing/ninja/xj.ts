import Logger from "js-logger";
import { Cursor } from "../../cursor/Cursor";
import { Vec3 } from "../../Vec3";
import { NjVertex } from "../ninja";

const logger = Logger.get("data_formats/parsing/ninja/xj");

// TODO:
// - textures
// - colors
// - bump maps
// - animation

export type XjModel = {
    type: "xj";
    vertices: NjVertex[];
    meshes: XjMesh[];
    collision_sphere_position: Vec3;
    collision_sphere_radius: number;
};

export type XjMesh = {
    indices: number[];
};

export function parse_xj_model(cursor: Cursor): XjModel {
    cursor.seek(4); // Flags according to QEdit, seemingly always 0.
    const vertex_info_table_offset = cursor.u32();
    const vertex_info_count = cursor.u32();
    const triangle_strip_table_offset = cursor.u32();
    const triangle_strip_count = cursor.u32();
    const transparent_triangle_strip_table_offset = cursor.u32();
    const transparent_triangle_strip_count = cursor.u32();
    const collision_sphere_position = cursor.vec3();
    const collision_sphere_radius = cursor.f32();

    const model: XjModel = {
        type: "xj",
        vertices: [],
        meshes: [],
        collision_sphere_position,
        collision_sphere_radius,
    };

    if (vertex_info_count >= 1) {
        if (vertex_info_count > 1) {
            logger.warn(`Vertex info count of ${vertex_info_count} was larger than expected.`);
        }

        cursor.seek_start(vertex_info_table_offset);
        cursor.seek(4); // Vertex type.
        const vertex_table_offset = cursor.u32();
        const vertex_size = cursor.u32();
        const vertex_count = cursor.u32();

        for (let i = 0; i < vertex_count; ++i) {
            cursor.seek_start(vertex_table_offset + i * vertex_size);

            const position = cursor.vec3();
            let normal: Vec3 | undefined;

            if (vertex_size === 28 || vertex_size === 32 || vertex_size === 36) {
                normal = cursor.vec3();
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

    if (triangle_strip_table_offset) {
        model.meshes.push(
            ...parse_triangle_strip_table(cursor, triangle_strip_table_offset, triangle_strip_count)
        );
    }

    if (transparent_triangle_strip_table_offset) {
        model.meshes.push(
            ...parse_triangle_strip_table(
                cursor,
                transparent_triangle_strip_table_offset,
                transparent_triangle_strip_count
            )
        );
    }

    return model;
}

function parse_triangle_strip_table(
    cursor: Cursor,
    triangle_strip_list_offset: number,
    triangle_strip_count: number
): XjMesh[] {
    const strips: XjMesh[] = [];

    for (let i = 0; i < triangle_strip_count; ++i) {
        cursor.seek_start(triangle_strip_list_offset + i * 20);

        cursor.seek(8); // Skipping flag_and_texture_id_offset and data_type?
        const index_list_offset = cursor.u32();
        const index_count = cursor.u32();

        cursor.seek_start(index_list_offset);
        const indices = cursor.u16_array(index_count);

        strips.push({
            indices,
        });
    }

    return strips;
}
