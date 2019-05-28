import {
    BufferAttribute,
    BufferGeometry,
    DoubleSide,
    Face3,
    Geometry,
    Mesh,
    MeshBasicMaterial,
    MeshLambertMaterial,
    Object3D,
    TriangleStripDrawMode,
    Vector3
} from 'three';
import { Vec3, Section } from '../../domain';

export function parse_c_rel(array_buffer: ArrayBuffer): Object3D {
    const dv = new DataView(array_buffer);

    const object = new Object3D();
    const materials = [
        // Wall
        new MeshBasicMaterial({
            color: 0x80C0D0,
            transparent: true,
            opacity: 0.25
        }),
        // Ground
        new MeshLambertMaterial({
            color: 0x50D0D0,
            side: DoubleSide
        }),
        // Vegetation
        new MeshLambertMaterial({
            color: 0x50B070,
            side: DoubleSide
        }),
        // Section transition zone
        new MeshLambertMaterial({
            color: 0x604080,
            side: DoubleSide
        })
    ];
    const wireframe_materials = [
        // Wall
        new MeshBasicMaterial({
            color: 0x90D0E0,
            wireframe: true,
            transparent: true,
            opacity: 0.3,
        }),
        // Ground
        new MeshBasicMaterial({
            color: 0x60F0F0,
            wireframe: true
        }),
        // Vegetation
        new MeshBasicMaterial({
            color: 0x60C080,
            wireframe: true
        }),
        // Section transition zone
        new MeshBasicMaterial({
            color: 0x705090,
            wireframe: true
        })
    ];

    const main_block_offset = dv.getUint32(dv.byteLength - 16, true);
    const main_offset_table_offset = dv.getUint32(main_block_offset, true);

    for (
        let i = main_offset_table_offset;
        i === main_offset_table_offset || dv.getUint32(i) !== 0;
        i += 24
    ) {
        const block_geometry = new Geometry();

        const block_trailer_offset = dv.getUint32(i, true);
        const vertex_count = dv.getUint32(block_trailer_offset, true);
        const vertex_table_offset = dv.getUint32(block_trailer_offset + 4, true);
        const vertex_table_end = vertex_table_offset + 12 * vertex_count;
        const triangle_count = dv.getUint32(block_trailer_offset + 8, true);
        const triangle_table_offset = dv.getUint32(block_trailer_offset + 12, true);
        const triangle_table_end = triangle_table_offset + 36 * triangle_count;

        for (let j = vertex_table_offset; j < vertex_table_end; j += 12) {
            const x = dv.getFloat32(j, true);
            const y = dv.getFloat32(j + 4, true);
            const z = dv.getFloat32(j + 8, true);

            block_geometry.vertices.push(new Vector3(x, y, z));
        }

        for (let j = triangle_table_offset; j < triangle_table_end; j += 36) {
            const v1 = dv.getUint16(j, true);
            const v2 = dv.getUint16(j + 2, true);
            const v3 = dv.getUint16(j + 4, true);
            const flags = dv.getUint16(j + 6, true);
            const n = new Vector3(
                dv.getFloat32(j + 8, true),
                dv.getFloat32(j + 12, true),
                dv.getFloat32(j + 16, true)
            );
            const is_section_transition = flags & 0b1000000;
            const is_vegetation = flags & 0b10000;
            const is_ground = flags & 0b1;
            const color_index = is_section_transition ? 3 : (is_vegetation ? 2 : (is_ground ? 1 : 0));

            block_geometry.faces.push(new Face3(v1, v2, v3, n, undefined, color_index));
        }

        const mesh = new Mesh(block_geometry, materials);
        mesh.renderOrder = 1;
        object.add(mesh);

        const wireframe_mesh = new Mesh(block_geometry, wireframe_materials);
        wireframe_mesh.renderOrder = 2;
        object.add(wireframe_mesh);
    }

    return object;
}

export function parse_n_rel(
    array_buffer: ArrayBuffer
): { sections: Section[], object_3d: Object3D } {
    const dv = new DataView(array_buffer);
    const sections = new Map();

    const object = new Object3D();

    const main_block_offset = dv.getUint32(dv.byteLength - 16, true);
    const section_count = dv.getUint32(main_block_offset + 8, true);
    const section_table_offset = dv.getUint32(main_block_offset + 16, true);
    // const texture_name_offset = dv.getUint32(main_block_offset + 20, true);

    for (
        let i = section_table_offset;
        i < section_table_offset + section_count * 52;
        i += 52
    ) {
        const section_id = dv.getInt32(i, true);
        const section_x = dv.getFloat32(i + 4, true);
        const section_y = dv.getFloat32(i + 8, true);
        const section_z = dv.getFloat32(i + 12, true);
        const section_rotation = dv.getInt32(i + 20, true) / 0xFFFF * 2 * Math.PI;
        const section = new Section(
            section_id,
            new Vec3(section_x, section_y, section_z),
            section_rotation);
        sections.set(section_id, section);

        const index_lists_list = [];
        const position_lists_list = [];
        const normal_lists_list = [];

        const simple_geometry_offset_table_offset = dv.getUint32(i + 32, true);
        // const complex_geometry_offset_table_offset = dv.getUint32(i + 36, true);
        const simple_geometry_offset_count = dv.getUint32(i + 40, true);
        // const complex_geometry_offset_count = dv.getUint32(i + 44, true);

        // console.log(`section id: ${section_id}, section rotation: ${section_rotation}, simple vertices: ${simple_geometry_offset_count}, complex vertices: ${complex_geometry_offset_count}`);

        for (
            let j = simple_geometry_offset_table_offset;
            j < simple_geometry_offset_table_offset + simple_geometry_offset_count * 16;
            j += 16
        ) {
            let offset = dv.getUint32(j, true);
            const flags = dv.getUint32(j + 12, true);

            if (flags & 0b100) {
                offset = dv.getUint32(offset, true);
            }

            const geometry_offset = dv.getUint32(offset + 4, true);

            if (geometry_offset > 0) {
                const vertex_info_table_offset = dv.getUint32(geometry_offset + 4, true);
                const vertex_info_count = dv.getUint32(geometry_offset + 8, true);
                const triangle_strip_table_offset = dv.getUint32(geometry_offset + 12, true);
                const triangle_strip_count = dv.getUint32(geometry_offset + 16, true);
                // const transparent_object_table_offset = dv.getUint32(block_offset + 20, true);
                // const transparent_object_count = dv.getUint32(block_offset + 24, true);

                // console.log(`block offset: ${block_offset}, vertex info count: ${vertex_info_count}, object table offset ${object_table_offset}, object count: ${object_count}, transparent object count: ${transparent_object_count}`);

                const geom_index_lists = [];

                for (
                    let k = triangle_strip_table_offset;
                    k < triangle_strip_table_offset + triangle_strip_count * 20;
                    k += 20
                ) {
                    // const flag_and_texture_id_offset = dv.getUint32(k, true);
                    // const data_type = dv.getUint32(k + 4, true);
                    const triangle_strip_index_table_offset = dv.getUint32(k + 8, true);
                    const triangle_strip_index_count = dv.getUint32(k + 12, true);

                    const triangle_strip_indices = [];

                    for (
                        let l = triangle_strip_index_table_offset;
                        l < triangle_strip_index_table_offset + triangle_strip_index_count * 2;
                        l += 2
                    ) {
                        triangle_strip_indices.push(dv.getUint16(l, true));
                    }

                    geom_index_lists.push(triangle_strip_indices);

                    // TODO: Read texture info.
                }

                // TODO: Do the previous for the transparent index table.

                // Assume vertex_info_count == 1. TODO: Does that make sense?
                if (vertex_info_count > 1) {
                    console.warn(`Vertex info count of ${vertex_info_count} was larger than expected.`);
                }

                // const vertex_type = dv.getUint32(vertex_info_table_offset, true);
                const vertex_table_offset = dv.getUint32(vertex_info_table_offset + 4, true);
                const vertex_size = dv.getUint32(vertex_info_table_offset + 8, true);
                const vertex_count = dv.getUint32(vertex_info_table_offset + 12, true);

                // console.log(`vertex type: ${vertex_type}, vertex size: ${vertex_size}, vertex count: ${vertex_count}`);

                const geom_positions = [];
                const geom_normals = [];

                for (
                    let k = vertex_table_offset;
                    k < vertex_table_offset + vertex_count * vertex_size;
                    k += vertex_size
                ) {
                    let n_x, n_y, n_z;

                    switch (vertex_size) {
                        case 16:
                        case 24:
                            // TODO: are these values sensible?
                            n_x = 0;
                            n_y = 1;
                            n_z = 0;
                            break;
                        case 28:
                        case 36:
                            n_x = dv.getFloat32(k + 12, true);
                            n_y = dv.getFloat32(k + 16, true);
                            n_z = dv.getFloat32(k + 20, true);
                            // TODO: color, texture coords.
                            break;
                        default:
                            console.error(`Unexpected vertex size of ${vertex_size}.`);
                            continue;
                    }

                    const x = dv.getFloat32(k, true);
                    const y = dv.getFloat32(k + 4, true);
                    const z = dv.getFloat32(k + 8, true);
                    const rotated_x = section.cos_y_axis_rotation * x + section.sin_y_axis_rotation * z;
                    const rotated_z = -section.sin_y_axis_rotation * x + section.cos_y_axis_rotation * z;

                    geom_positions.push(section_x + rotated_x);
                    geom_positions.push(section_y + y);
                    geom_positions.push(section_z + rotated_z);
                    geom_normals.push(n_x);
                    geom_normals.push(n_y);
                    geom_normals.push(n_z);
                }

                index_lists_list.push(geom_index_lists);
                position_lists_list.push(geom_positions);
                normal_lists_list.push(geom_normals);
            } else {
                // console.error(`Block offset at ${offset + 4} was ${block_offset}.`);
            }
        }

        // function v_equal(v, w) {
        //     return v[0] === w[0] && v[1] === w[1] && v[2] === w[2];
        // }

        for (let i = 0; i < position_lists_list.length; ++i) {
            const positions = position_lists_list[i];
            const normals = normal_lists_list[i];
            const geom_index_lists = index_lists_list[i];
            // const indices = [];

            geom_index_lists.forEach(object_indices => {
                // for (let j = 2; j < object_indices.length; ++j) {
                //     const a = object_indices[j - 2];
                //     const b = object_indices[j - 1];
                //     const c = object_indices[j];

                //     if (a !== b && a !== c && b !== c) {
                //         const ap = positions.slice(3 * a, 3 * a + 3);
                //         const bp = positions.slice(3 * b, 3 * b + 3);
                //         const cp = positions.slice(3 * c, 3 * c + 3);

                //         if (!v_equal(ap, bp) && !v_equal(ap, cp) && !v_equal(bp, cp)) {
                //             if (j % 2 === 0) {
                //                 indices.push(a);
                //                 indices.push(b);
                //                 indices.push(c);
                //             } else {
                //                 indices.push(b);
                //                 indices.push(a);
                //                 indices.push(c);
                //             }
                //         }
                //     }
                // }

                const geometry = new BufferGeometry();
                geometry.addAttribute(
                    'position', new BufferAttribute(new Float32Array(positions), 3));
                geometry.addAttribute(
                    'normal', new BufferAttribute(new Float32Array(normals), 3));
                geometry.setIndex(new BufferAttribute(new Uint16Array(object_indices), 1));

                const mesh = new Mesh(
                    geometry,
                    new MeshLambertMaterial({
                        color: 0x44aaff,
                        // transparent: true,
                        opacity: 0.25,
                        side: DoubleSide
                    })
                );
                mesh.setDrawMode(TriangleStripDrawMode);
                mesh.userData.section = section;
                object.add(mesh);
            });

            // const geometry = new BufferGeometry();
            // geometry.addAttribute(
            //     'position', new BufferAttribute(new Float32Array(positions), 3));
            // geometry.addAttribute(
            //     'normal', new BufferAttribute(new Float32Array(normals), 3));
            // geometry.setIndex(new BufferAttribute(new Uint16Array(indices), 1));

            // const mesh = new Mesh(
            //     geometry,
            //     new MeshLambertMaterial({
            //         color: 0x44aaff,
            //         transparent: true,
            //         opacity: 0.25,
            //         side: DoubleSide
            //     })
            // );
            // object.add(mesh);

            // const wireframe_mesh = new Mesh(
            //     geometry,
            //     new MeshBasicMaterial({
            //         color: 0x88ccff,
            //         wireframe: true,
            //         transparent: true,
            //         opacity: 0.75,
            //     })
            // );
            // wireframe_mesh.setDrawMode(THREE.TriangleStripDrawMode);
            // object.add(wireframe_mesh);
        }
    }

    return {
        sections: [...sections.values()].sort((a, b) => a.id - b.id),
        object_3d: object
    };
}
