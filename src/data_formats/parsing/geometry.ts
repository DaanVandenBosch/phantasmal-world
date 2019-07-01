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
import Logger from 'js-logger';

const logger = Logger.get('data_formats/parsing/geometry');

export function parseCRel(arrayBuffer: ArrayBuffer): Object3D {
    const dv = new DataView(arrayBuffer);

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
    const wireframeMaterials = [
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

    const mainBlockOffset = dv.getUint32(dv.byteLength - 16, true);
    const mainOffsetTableOffset = dv.getUint32(mainBlockOffset, true);

    for (
        let i = mainOffsetTableOffset;
        i === mainOffsetTableOffset || dv.getUint32(i) !== 0;
        i += 24
    ) {
        const blockGeometry = new Geometry();

        const blockTrailerOffset = dv.getUint32(i, true);
        const vertexCount = dv.getUint32(blockTrailerOffset, true);
        const vertexTableOffset = dv.getUint32(blockTrailerOffset + 4, true);
        const vertexTableEnd = vertexTableOffset + 12 * vertexCount;
        const triangleCount = dv.getUint32(blockTrailerOffset + 8, true);
        const triangleTableOffset = dv.getUint32(blockTrailerOffset + 12, true);
        const triangleTableEnd = triangleTableOffset + 36 * triangleCount;

        for (let j = vertexTableOffset; j < vertexTableEnd; j += 12) {
            const x = dv.getFloat32(j, true);
            const y = dv.getFloat32(j + 4, true);
            const z = dv.getFloat32(j + 8, true);

            blockGeometry.vertices.push(new Vector3(x, y, z));
        }

        for (let j = triangleTableOffset; j < triangleTableEnd; j += 36) {
            const v1 = dv.getUint16(j, true);
            const v2 = dv.getUint16(j + 2, true);
            const v3 = dv.getUint16(j + 4, true);
            const flags = dv.getUint16(j + 6, true);
            const n = new Vector3(
                dv.getFloat32(j + 8, true),
                dv.getFloat32(j + 12, true),
                dv.getFloat32(j + 16, true)
            );
            const isSectionTransition = flags & 0b1000000;
            const isVegetation = flags & 0b10000;
            const isGround = flags & 0b1;
            const colorIndex = isSectionTransition ? 3 : (isVegetation ? 2 : (isGround ? 1 : 0));

            blockGeometry.faces.push(new Face3(v1, v2, v3, n, undefined, colorIndex));
        }

        const mesh = new Mesh(blockGeometry, materials);
        mesh.renderOrder = 1;
        object.add(mesh);

        const wireframeMesh = new Mesh(blockGeometry, wireframeMaterials);
        wireframeMesh.renderOrder = 2;
        object.add(wireframeMesh);
    }

    return object;
}

export function parseNRel(
    arrayBuffer: ArrayBuffer
): { sections: Section[], object3d: Object3D } {
    const dv = new DataView(arrayBuffer);
    const sections = new Map();

    const object = new Object3D();

    const mainBlockOffset = dv.getUint32(dv.byteLength - 16, true);
    const sectionCount = dv.getUint32(mainBlockOffset + 8, true);
    const sectionTableOffset = dv.getUint32(mainBlockOffset + 16, true);
    // const textureNameOffset = dv.getUint32(mainBlockOffset + 20, true);

    for (
        let i = sectionTableOffset;
        i < sectionTableOffset + sectionCount * 52;
        i += 52
    ) {
        const sectionId = dv.getInt32(i, true);
        const sectionX = dv.getFloat32(i + 4, true);
        const sectionY = dv.getFloat32(i + 8, true);
        const sectionZ = dv.getFloat32(i + 12, true);
        const sectionRotation = dv.getInt32(i + 20, true) / 0xFFFF * 2 * Math.PI;
        const section = new Section(
            sectionId,
            new Vec3(sectionX, sectionY, sectionZ),
            sectionRotation
        );
        sections.set(sectionId, section);

        const indexListsList = [];
        const positionListsList = [];
        const normalListsList = [];

        const simpleGeometryOffsetTableOffset = dv.getUint32(i + 32, true);
        // const complexGeometryOffsetTableOffset = dv.getUint32(i + 36, true);
        const simpleGeometryOffsetCount = dv.getUint32(i + 40, true);
        // const complexGeometryOffsetCount = dv.getUint32(i + 44, true);

        // logger.log(`section id: ${sectionId}, section rotation: ${sectionRotation}, simple vertices: ${simpleGeometryOffsetCount}, complex vertices: ${complexGeometryOffsetCount}`);

        for (
            let j = simpleGeometryOffsetTableOffset;
            j < simpleGeometryOffsetTableOffset + simpleGeometryOffsetCount * 16;
            j += 16
        ) {
            let offset = dv.getUint32(j, true);
            const flags = dv.getUint32(j + 12, true);

            if (flags & 0b100) {
                offset = dv.getUint32(offset, true);
            }

            const geometryOffset = dv.getUint32(offset + 4, true);

            if (geometryOffset > 0) {
                const vertexInfoTableOffset = dv.getUint32(geometryOffset + 4, true);
                const vertexInfoCount = dv.getUint32(geometryOffset + 8, true);
                const triangleStripTableOffset = dv.getUint32(geometryOffset + 12, true);
                const triangleStripCount = dv.getUint32(geometryOffset + 16, true);
                // const transparentObjectTableOffset = dv.getUint32(blockOffset + 20, true);
                // const transparentObjectCount = dv.getUint32(blockOffset + 24, true);

                // logger.log(`block offset: ${blockOffset}, vertex info count: ${vertexInfoCount}, object table offset ${objectTableOffset}, object count: ${objectCount}, transparent object count: ${transparentObjectCount}`);

                const geomIndexLists = [];

                for (
                    let k = triangleStripTableOffset;
                    k < triangleStripTableOffset + triangleStripCount * 20;
                    k += 20
                ) {
                    // const flagAndTextureIdOffset = dv.getUint32(k, true);
                    // const dataType = dv.getUint32(k + 4, true);
                    const triangleStripIndexTableOffset = dv.getUint32(k + 8, true);
                    const triangleStripIndexCount = dv.getUint32(k + 12, true);

                    const triangleStripIndices = [];

                    for (
                        let l = triangleStripIndexTableOffset;
                        l < triangleStripIndexTableOffset + triangleStripIndexCount * 2;
                        l += 2
                    ) {
                        triangleStripIndices.push(dv.getUint16(l, true));
                    }

                    geomIndexLists.push(triangleStripIndices);

                    // TODO: Read texture info.
                }

                // TODO: Do the previous for the transparent index table.

                // Assume vertexInfoCount == 1. TODO: Does that make sense?
                if (vertexInfoCount > 1) {
                    logger.warn(`Vertex info count of ${vertexInfoCount} was larger than expected.`);
                }

                // const vertexType = dv.getUint32(vertexInfoTableOffset, true);
                const vertexTableOffset = dv.getUint32(vertexInfoTableOffset + 4, true);
                const vertexSize = dv.getUint32(vertexInfoTableOffset + 8, true);
                const vertexCount = dv.getUint32(vertexInfoTableOffset + 12, true);

                // logger.log(`vertex type: ${vertexType}, vertex size: ${vertexSize}, vertex count: ${vertexCount}`);

                const geomPositions = [];
                const geomNormals = [];

                for (
                    let k = vertexTableOffset;
                    k < vertexTableOffset + vertexCount * vertexSize;
                    k += vertexSize
                ) {
                    let nX, nY, nZ;

                    switch (vertexSize) {
                        case 16:
                        case 24:
                            // TODO: are these values sensible?
                            nX = 0;
                            nY = 1;
                            nZ = 0;
                            break;
                        case 28:
                        case 36:
                            nX = dv.getFloat32(k + 12, true);
                            nY = dv.getFloat32(k + 16, true);
                            nZ = dv.getFloat32(k + 20, true);
                            // TODO: color, texture coords.
                            break;
                        default:
                            logger.error(`Unexpected vertex size of ${vertexSize}.`);
                            continue;
                    }

                    const x = dv.getFloat32(k, true);
                    const y = dv.getFloat32(k + 4, true);
                    const z = dv.getFloat32(k + 8, true);
                    const rotatedX = section.cos_y_axis_rotation * x + section.sin_y_axis_rotation * z;
                    const rotatedZ = -section.sin_y_axis_rotation * x + section.cos_y_axis_rotation * z;

                    geomPositions.push(sectionX + rotatedX);
                    geomPositions.push(sectionY + y);
                    geomPositions.push(sectionZ + rotatedZ);
                    geomNormals.push(nX);
                    geomNormals.push(nY);
                    geomNormals.push(nZ);
                }

                indexListsList.push(geomIndexLists);
                positionListsList.push(geomPositions);
                normalListsList.push(geomNormals);
            } else {
                // logger.error(`Block offset at ${offset + 4} was ${blockOffset}.`);
            }
        }

        // function vEqual(v, w) {
        //     return v[0] === w[0] && v[1] === w[1] && v[2] === w[2];
        // }

        for (let i = 0; i < positionListsList.length; ++i) {
            const positions = positionListsList[i];
            const normals = normalListsList[i];
            const geomIndexLists = indexListsList[i];
            // const indices = [];

            geomIndexLists.forEach(objectIndices => {
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
                geometry.addAttribute(
                    'position', new BufferAttribute(new Float32Array(positions), 3));
                geometry.addAttribute(
                    'normal', new BufferAttribute(new Float32Array(normals), 3));
                geometry.setIndex(new BufferAttribute(new Uint16Array(objectIndices), 1));

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
        object3d: object
    };
}
