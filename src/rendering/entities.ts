import {
    BufferGeometry,
    DoubleSide,
    Mesh,
    MeshBasicMaterial,
    MeshLambertMaterial,
    Texture,
    Material,
} from "three";
import { QuestEntity, QuestNpc, QuestObject } from "../domain";

export const OBJECT_COLOR = 0xffff00;
export const OBJECT_HOVER_COLOR = 0xffdf3f;
export const OBJECT_SELECTED_COLOR = 0xffaa00;
export const NPC_COLOR = 0xff0000;
export const NPC_HOVER_COLOR = 0xff3f5f;
export const NPC_SELECTED_COLOR = 0xff0054;

export function create_object_mesh(
    object: QuestObject,
    geometry: BufferGeometry,
    textures: Texture[]
): Mesh {
    return create_mesh(object, geometry, textures, OBJECT_COLOR, "Object");
}

export function create_npc_mesh(
    npc: QuestNpc,
    geometry: BufferGeometry,
    textures: Texture[]
): Mesh {
    return create_mesh(npc, geometry, textures, NPC_COLOR, "NPC");
}

function create_mesh(
    entity: QuestEntity,
    geometry: BufferGeometry,
    textures: Texture[],
    color: number,
    type: string
): Mesh {
    const max_mat_idx = geometry.groups.reduce((max, g) => Math.max(max, g.materialIndex || 0), 0);

    const materials: Material[] = [
        new MeshBasicMaterial({
            color,
            side: DoubleSide,
        }),
    ];

    materials.push(
        ...textures.map(
            tex =>
                new MeshLambertMaterial({
                    map: tex,
                    side: DoubleSide,
                    alphaTest: 0.5,
                })
        )
    );

    for (let i = materials.length - 1; i < max_mat_idx; ++i) {
        materials.push(
            new MeshLambertMaterial({
                color,
                side: DoubleSide,
            })
        );
    }

    const mesh = new Mesh(geometry, materials);
    mesh.name = type;
    mesh.userData.entity = entity;

    const { x, y, z } = entity.position;
    mesh.position.set(x, y, z);
    const rot = entity.rotation;
    mesh.rotation.set(rot.x, rot.y, rot.z);

    return mesh;
}
