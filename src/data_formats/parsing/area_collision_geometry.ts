import { Cursor } from "../cursor/Cursor";
import { Vec3 } from "../Vec3";

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
    cursor.seek_end(16);
    const main_block_offset = cursor.u32();
    cursor.seek_start(main_block_offset);
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
            const x = cursor.f32();
            const y = cursor.f32();
            const z = cursor.f32();

            mesh.vertices.push(new Vec3(x, y, z));
        }

        cursor.seek_start(triangle_table_offset);

        for (let i = 0; i < triangle_count; i++) {
            const v1 = cursor.u16();
            const v2 = cursor.u16();
            const v3 = cursor.u16();
            const flags = cursor.u16();
            const n_x = cursor.f32();
            const n_y = cursor.f32();
            const n_z = cursor.f32();
            cursor.seek(16);

            mesh.triangles.push({
                indices: [v1, v2, v3],
                flags,
                normal: new Vec3(n_x, n_y, n_z),
            });
        }

        cursor.seek_start(start_pos + 24);
    }

    return object;
}
