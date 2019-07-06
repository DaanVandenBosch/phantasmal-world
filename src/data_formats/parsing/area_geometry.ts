import Logger from "js-logger";
import {
    BufferGeometry,
    DoubleSide,
    Float32BufferAttribute,
    Mesh,
    MeshLambertMaterial,
    Object3D,
    TriangleStripDrawMode,
    Uint16BufferAttribute,
} from "three";
import { Section } from "../../domain";
import { Vec3 } from "../Vec3";

const logger = Logger.get("data_formats/parsing/area_geometry");

export function parse_area_geometry(
    array_buffer: ArrayBuffer
): { sections: Section[]; object_3d: Object3D } {
    const dv = new DataView(array_buffer);
    const sections = new Map();

    const object = new Object3D();

    const main_block_offset = dv.getUint32(dv.byteLength - 16, true);
    const section_count = dv.getUint32(main_block_offset + 8, true);
    const section_table_offset = dv.getUint32(main_block_offset + 16, true);
    // const texture_name_offset = dv.getUint32(main_block_offset + 20, true);

    for (let i = section_table_offset; i < section_table_offset + section_count * 52; i += 52) {
        const section_id = dv.getInt32(i, true);
        const section_x = dv.getFloat32(i + 4, true);
        const section_y = dv.getFloat32(i + 8, true);
        const section_z = dv.getFloat32(i + 12, true);
        const section_rotation = (dv.getInt32(i + 20, true) / 0xffff) * 2 * Math.PI;
        const section = new Section(
            section_id,
            new Vec3(section_x, section_y, section_z),
            section_rotation
        );
        sections.set(section_id, section);

        const index_lists_list = [];
        const position_lists_list = [];
        const normal_lists_list = [];

        const simple_geometry_offset_table_offset = dv.getUint32(i + 32, true);
        // const complex_geometry_offset_table_offset = dv.getUint32(i + 36, true);
        const simple_geometry_offset_count = dv.getUint32(i + 40, true);
        // const complex_geometry_offset_count = dv.getUint32(i + 44, true);

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
                // const transparent_object_table_offset = dv.getUint32(blockOffset + 20, true);
                // const transparent_object_count = dv.getUint32(blockOffset + 24, true);

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

                // Assume vertexInfoCount == 1. TODO: Does that make sense?
                if (vertex_info_count > 1) {
                    logger.warn(
                        `Vertex info count of ${vertex_info_count} was larger than expected.`
                    );
                }

                // const vertex_type = dv.getUint32(vertexInfoTableOffset, true);
                const vertex_table_offset = dv.getUint32(vertex_info_table_offset + 4, true);
                const vertex_size = dv.getUint32(vertex_info_table_offset + 8, true);
                const vertex_count = dv.getUint32(vertex_info_table_offset + 12, true);

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
                            logger.error(`Unexpected vertex size of ${vertex_size}.`);
                            continue;
                    }

                    const x = dv.getFloat32(k, true);
                    const y = dv.getFloat32(k + 4, true);
                    const z = dv.getFloat32(k + 8, true);
                    const rotated_x =
                        section.cos_y_axis_rotation * x + section.sin_y_axis_rotation * z;
                    const rotated_z =
                        -section.sin_y_axis_rotation * x + section.cos_y_axis_rotation * z;

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
            }
        }

        // function vEqual(v, w) {
        //     return v[0] === w[0] && v[1] === w[1] && v[2] === w[2];
        // }

        for (let i = 0; i < position_lists_list.length; ++i) {
            const positions = position_lists_list[i];
            const normals = normal_lists_list[i];
            const geom_index_lists = index_lists_list[i];
            // const indices = [];

            geom_index_lists.forEach(object_indices => {
                // for (let j = 2; j < objectIndices.length; ++j) {
                //     const a = objectIndices[j - 2];
                //     const b = objectIndices[j - 1];
                //     const c = objectIndices[j];

                //     if (a !== b && a !== c && b !== c) {
                //         const ap = positions.slice(3 * a, 3 * a + 3);
                //         const bp = positions.slice(3 * b, 3 * b + 3);
                //         const cp = positions.slice(3 * c, 3 * c + 3);

                //         if (!vEqual(ap, bp) && !vEqual(ap, cp) && !vEqual(bp, cp)) {
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
                geometry.addAttribute("position", new Float32BufferAttribute(positions, 3));
                geometry.addAttribute("normal", new Float32BufferAttribute(normals, 3));
                geometry.setIndex(new Uint16BufferAttribute(object_indices, 1));

                const mesh = new Mesh(
                    geometry,
                    new MeshLambertMaterial({
                        color: 0x44aaff,
                        // transparent: true,
                        opacity: 0.25,
                        side: DoubleSide,
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

            // const wireframeMesh = new Mesh(
            //     geometry,
            //     new MeshBasicMaterial({
            //         color: 0x88ccff,
            //         wireframe: true,
            //         transparent: true,
            //         opacity: 0.75,
            //     })
            // );
            // wireframeMesh.setDrawMode(THREE.TriangleStripDrawMode);
            // object.add(wireframeMesh);
        }
    }

    return {
        sections: [...sections.values()].sort((a, b) => a.id - b.id),
        object_3d: object,
    };
}
