import {
    DoubleSide,
    Face3,
    Geometry,
    Group,
    Mesh,
    MeshBasicMaterial,
    MeshLambertMaterial,
    Object3D,
    Vector3,
    Color,
} from "three";
import { CollisionObject } from "../../../core/data_formats/parsing/area_collision_geometry";
import { RenderObject } from "../../../core/data_formats/parsing/area_geometry";
import { GeometryBuilder } from "../../../core/rendering/conversion/GeometryBuilder";
import { ninja_object_to_geometry_builder } from "../../../core/rendering/conversion/ninja_geometry";
import { Section } from "../../domain/Section";

const materials = [
    // Wall
    new MeshBasicMaterial({
        color: 0x80c0d0,
        transparent: true,
        opacity: 0.25,
        visible: false,
    }),
    // Ground
    new MeshLambertMaterial({
        color: 0xa0a0a0,
        side: DoubleSide,
    }),
    // Vegetation
    new MeshLambertMaterial({
        color: 0x50b070,
        side: DoubleSide,
    }),
    // Section transition zone
    new MeshLambertMaterial({
        color: 0x604080,
        side: DoubleSide,
    }),
];
const wireframe_materials = [
    // Wall
    new MeshBasicMaterial({
        color: 0x90d0e0,
        wireframe: true,
        transparent: true,
        opacity: 0.3,
        visible: false,
    }),
    // Ground
    new MeshBasicMaterial({
        color: 0xd0d0d0,
        wireframe: true,
    }),
    // Vegetation
    new MeshBasicMaterial({
        color: 0x80e0a0,
        wireframe: true,
    }),
    // Section transition zone
    new MeshBasicMaterial({
        color: 0x9070b0,
        wireframe: true,
    }),
];

export type AreaUserData = {
    section?: Section;
};

export function area_collision_geometry_to_object_3d(object: CollisionObject): Object3D {
    const group = new Group();

    for (const collision_mesh of object.meshes) {
        // Use Geometry and not BufferGeometry for better raycaster performance.
        const geom = new Geometry();

        for (const { x, y, z } of collision_mesh.vertices) {
            geom.vertices.push(new Vector3(x, y, z));
        }

        for (const { indices, flags, normal } of collision_mesh.triangles) {
            const is_section_transition = flags & 0b1000000;
            const is_vegetation = flags & 0b10000;
            const is_ground = flags & 0b1;
            const color_index = is_section_transition ? 3 : is_vegetation ? 2 : is_ground ? 1 : 0;

            geom.faces.push(
                new Face3(
                    indices[0],
                    indices[1],
                    indices[2],
                    new Vector3(normal.x, normal.y, normal.z),
                    undefined,
                    color_index,
                ),
            );
        }

        geom.computeBoundingBox();
        geom.computeBoundingSphere();

        const mesh = new Mesh(geom, materials);
        mesh.renderOrder = 1;
        group.add(mesh);

        const wireframe_mesh = new Mesh(geom, wireframe_materials);
        wireframe_mesh.renderOrder = 2;
        group.add(wireframe_mesh);
    }

    return group;
}

export function area_geometry_to_sections_and_object_3d(
    object: RenderObject,
): [Section[], Object3D] {
    const sections: Section[] = [];
    const group = new Group();
    let i = 0;

    for (const section of object.sections) {
        const builder = new GeometryBuilder();

        for (const object of section.objects) {
            ninja_object_to_geometry_builder(object, builder);
        }

        const mesh = new Mesh(
            builder.build(),
            new MeshBasicMaterial({
                color: new Color().setHSL((i++ % 7) / 7, 1, 0.5),
                transparent: true,
                opacity: 0.25,
                side: DoubleSide,
            }),
        );
        group.add(mesh);

        mesh.position.set(section.position.x, section.position.y, section.position.z);
        mesh.rotation.set(section.rotation.x, section.rotation.y, section.rotation.z);

        if (section.id >= 0) {
            const sec = new Section(section.id, section.position, section.rotation.y);
            sections.push(sec);
            (mesh.userData as AreaUserData).section = sec;
        }
    }

    return [sections, group];
}
