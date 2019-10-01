import Logger from "js-logger";
import { Cursor } from "../../cursor/Cursor";
import { Vec2, Vec3 } from "../../vector";

const logger = Logger.get("core/data_formats/parsing/ninja/xj");

// TODO:
// - vertex colors
// - bump maps

export type XjModel = {
    type: "xj";
    vertices: XjVertex[];
    meshes: XjMesh[];
    collision_sphere_position: Vec3;
    collision_sphere_radius: number;
};

export type XjVertex = {
    position: Vec3;
    normal?: Vec3;
    uv?: Vec2;
};

export type XjMesh = {
    material_properties: XjMaterialProperties;
    indices: number[];
};

export type XjMaterialProperties = {
    alpha_src?: number;
    alpha_dst?: number;
    texture_id?: number;
    diffuse_r?: number;
    diffuse_g?: number;
    diffuse_b?: number;
    diffuse_a?: number;
};

export function parse_xj_model(cursor: Cursor): XjModel {
    cursor.seek(4); // Flags according to QEdit, seemingly always 0.
    const vertex_info_table_offset = cursor.u32();
    const vertex_info_count = cursor.u32();
    const triangle_strip_table_offset = cursor.u32();
    const triangle_strip_count = cursor.u32();
    const transparent_triangle_strip_table_offset = cursor.u32();
    const transparent_triangle_strip_count = cursor.u32();
    const collision_sphere_position = cursor.vec3_f32();
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

        model.vertices.push(...parse_vertex_info_table(cursor, vertex_info_table_offset));
    }

    model.meshes.push(
        ...parse_triangle_strip_table(cursor, triangle_strip_table_offset, triangle_strip_count),
    );

    model.meshes.push(
        ...parse_triangle_strip_table(
            cursor,
            transparent_triangle_strip_table_offset,
            transparent_triangle_strip_count,
        ),
    );

    return model;
}

function parse_vertex_info_table(cursor: Cursor, vertex_info_table_offset: number): XjVertex[] {
    cursor.seek_start(vertex_info_table_offset);
    const vertex_type = cursor.u16();
    cursor.seek(2); // Flags?
    const vertex_table_offset = cursor.u32();
    const vertex_size = cursor.u32();
    const vertex_count = cursor.u32();
    const vertices: XjVertex[] = [];

    for (let i = 0; i < vertex_count; ++i) {
        cursor.seek_start(vertex_table_offset + i * vertex_size);

        const position = cursor.vec3_f32();
        let normal: Vec3 | undefined;
        let uv: Vec2 | undefined;

        switch (vertex_type) {
            case 3:
                normal = cursor.vec3_f32();
                uv = cursor.vec2_f32();
                break;
            case 4:
                // Skip 4 bytes.
                break;
            case 5:
                cursor.seek(4);
                uv = cursor.vec2_f32();
                break;
            case 6:
                normal = cursor.vec3_f32();
                // Skip 4 bytes.
                break;
            case 7:
                normal = cursor.vec3_f32();
                uv = cursor.vec2_f32();
                break;
            default:
                logger.warn(`Unknown vertex type ${vertex_type} with size ${vertex_size}.`);
                break;
        }

        vertices.push({
            position,
            normal,
            uv,
        });
    }

    return vertices;
}

function parse_triangle_strip_table(
    cursor: Cursor,
    triangle_strip_list_offset: number,
    triangle_strip_count: number,
): XjMesh[] {
    const strips: XjMesh[] = [];

    for (let i = 0; i < triangle_strip_count; ++i) {
        cursor.seek_start(triangle_strip_list_offset + i * 20);

        const material_table_offset = cursor.u32();
        const material_table_size = cursor.u32();
        const index_list_offset = cursor.u32();
        const index_count = cursor.u32();

        const material_properties = parse_triangle_strip_material_properties(
            cursor,
            material_table_offset,
            material_table_size,
        );

        cursor.seek_start(index_list_offset);
        const indices = cursor.u16_array(index_count);

        strips.push({
            material_properties,
            indices,
        });
    }

    return strips;
}

function parse_triangle_strip_material_properties(
    cursor: Cursor,
    offset: number,
    size: number,
): XjMaterialProperties {
    const props: XjMaterialProperties = {};

    for (let i = 0; i < size; ++i) {
        cursor.seek_start(offset + i * 16);

        const type = cursor.u32();

        switch (type) {
            case 2:
                props.alpha_src = cursor.u32();
                props.alpha_dst = cursor.u32();
                break;
            case 3:
                props.texture_id = cursor.u32();
                break;
            case 5:
                props.diffuse_r = cursor.u8();
                props.diffuse_g = cursor.u8();
                props.diffuse_b = cursor.u8();
                props.diffuse_a = cursor.u8();
                break;
        }
    }

    return props;
}
