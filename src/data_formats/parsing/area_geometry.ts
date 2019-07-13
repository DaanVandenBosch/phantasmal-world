import { Cursor } from "../cursor/Cursor";
import { Vec3 } from "../vector";
import { ANGLE_TO_RAD } from "./ninja";
import { parse_xj_model, XjModel } from "./ninja/xj";
import { parse_rel } from "./rel";

export type RenderObject = {
    sections: RenderSection[];
};

export type RenderSection = {
    id: number;
    position: Vec3;
    rotation: Vec3;
    models: XjModel[];
};

export type Vertex = {
    position: Vec3;
    normal?: Vec3;
};

export function parse_area_geometry(cursor: Cursor): RenderObject {
    const sections: RenderSection[] = [];

    cursor.seek_end(16);

    const { data_offset } = parse_rel(cursor, false);
    cursor.seek_start(data_offset);
    cursor.seek(8); // Format "fmt2" in UTF-16.
    const section_count = cursor.u32();
    cursor.seek(4);
    const section_table_offset = cursor.u32();
    // const texture_name_offset = cursor.u32();

    for (let i = 0; i < section_count; i++) {
        cursor.seek_start(section_table_offset + 52 * i);

        const section_id = cursor.i32();
        const section_position = cursor.vec3_f32();
        const section_rotation = new Vec3(
            cursor.u32() * ANGLE_TO_RAD,
            cursor.u32() * ANGLE_TO_RAD,
            cursor.u32() * ANGLE_TO_RAD
        );

        cursor.seek(4);

        const simple_geometry_offset_table_offset = cursor.u32();
        // const animated_geometry_offset_table_offset = cursor.u32();
        cursor.seek(4);
        const simple_geometry_offset_count = cursor.u32();
        // const animated_geometry_offset_count = cursor.u32();
        // Ignore animated_geometry_offset_count and the last 4 bytes.

        const models = parse_geometry_table(
            cursor,
            simple_geometry_offset_table_offset,
            simple_geometry_offset_count
        );

        sections.push({
            id: section_id,
            position: section_position,
            rotation: section_rotation,
            models,
        });
    }

    return { sections };
}

function parse_geometry_table(
    cursor: Cursor,
    table_offset: number,
    table_entry_count: number
): XjModel[] {
    const models: XjModel[] = [];

    for (let i = 0; i < table_entry_count; i++) {
        cursor.seek_start(table_offset + 16 * i);

        let offset = cursor.u32();
        cursor.seek(8);
        const flags = cursor.u32();

        if (flags & 0b100) {
            offset = cursor.seek_start(offset).u32();
        }

        cursor.seek_start(offset + 4);
        const geometry_offset = cursor.u32();

        if (geometry_offset > 0) {
            cursor.seek_start(geometry_offset);
            models.push(parse_xj_model(cursor));
        }
    }

    return models;
}
