import { Cursor } from "../block/cursor/Cursor";
import { Vec3 } from "../vector";
import { NjObject, parse_xj_object } from "./ninja";
import { XjModel } from "./ninja/xj";
import { parse_rel } from "./rel";
import { angle_to_rad } from "./ninja/angle";

export type RenderObject = {
    sections: RenderSection[];
};

export type RenderSection = {
    id: number;
    position: Vec3;
    rotation: Vec3;
    objects: NjObject<XjModel>[];
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
        const section_rotation = {
            x: angle_to_rad(cursor.u32()),
            y: angle_to_rad(cursor.u32()),
            z: angle_to_rad(cursor.u32()),
        };

        cursor.seek(4);

        const simple_geometry_offset_table_offset = cursor.u32();
        // const animated_geometry_offset_table_offset = cursor.u32();
        cursor.seek(4);
        const simple_geometry_offset_count = cursor.u32();
        // const animated_geometry_offset_count = cursor.u32();
        // Ignore animated_geometry_offset_count and the last 4 bytes.

        const objects = parse_geometry_table(
            cursor,
            simple_geometry_offset_table_offset,
            simple_geometry_offset_count,
        );

        sections.push({
            id: section_id,
            position: section_position,
            rotation: section_rotation,
            objects,
        });
    }

    return { sections };
}

// TODO: don't reparse the same objects multiple times. Create DAG instead of tree.
function parse_geometry_table(
    cursor: Cursor,
    table_offset: number,
    table_entry_count: number,
): NjObject<XjModel>[] {
    const objects: NjObject<XjModel>[] = [];

    for (let i = 0; i < table_entry_count; i++) {
        cursor.seek_start(table_offset + 16 * i);

        let offset = cursor.u32();
        cursor.seek(8);
        const flags = cursor.u32();

        if (flags & 0b100) {
            offset = cursor.seek_start(offset).u32();
        }

        cursor.seek_start(offset);
        objects.push(...parse_xj_object(cursor));
    }

    return objects;
}
