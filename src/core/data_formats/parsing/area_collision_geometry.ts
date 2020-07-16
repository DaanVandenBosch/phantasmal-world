import { Cursor } from "../block/cursor/Cursor";
import { Vec3 } from "../vector";
import { parse_rel } from "./rel";

export type CollisionObject = {
    meshes: CollisionMesh[];
};

export type CollisionMesh = {
    vertices: Vec3[];
    triangles: CollisionTriangle[];
};

export type CollisionTriangle = {
    indices: [number, number, number];
    flags: number;
    normal: Vec3;
};

export function parse_area_collision_geometry(cursor: Cursor): CollisionObject {
    const { data_offset } = parse_rel(cursor, false);
    cursor.seek_start(data_offset);
    const main_offset_table_offset = cursor.u32();
    cursor.seek_start(main_offset_table_offset);

    const object: CollisionObject = {
        meshes: [],
    };

    while (cursor.bytes_left) {
        const start_pos = cursor.position;

        const block_trailer_offset = cursor.u32();

        if (block_trailer_offset === 0) {
            break;
        }

        const mesh: CollisionMesh = {
            vertices: [],
            triangles: [],
        };
        object.meshes.push(mesh);

        cursor.seek_start(block_trailer_offset);

        const vertex_count = cursor.u32();
        const vertex_table_offset = cursor.u32();
        const triangle_count = cursor.u32();
        const triangle_table_offset = cursor.u32();

        cursor.seek_start(vertex_table_offset);

        for (let i = 0; i < vertex_count; i++) {
            mesh.vertices.push(cursor.vec3_f32());
        }

        cursor.seek_start(triangle_table_offset);

        for (let i = 0; i < triangle_count; i++) {
            const v1 = cursor.u16();
            const v2 = cursor.u16();
            const v3 = cursor.u16();
            const flags = cursor.u16();
            const normal = cursor.vec3_f32();
            cursor.seek(16);

            mesh.triangles.push({
                indices: [v1, v2, v3],
                flags,
                normal,
            });
        }

        cursor.seek_start(start_pos + 24);
    }

    return object;
}
