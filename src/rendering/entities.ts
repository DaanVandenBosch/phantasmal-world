import { BufferGeometry, DoubleSide, Mesh, MeshLambertMaterial } from "three";
import { QuestEntity, QuestNpc, QuestObject } from "../domain";

export const OBJECT_COLOR = 0xffff00;
export const OBJECT_HOVER_COLOR = 0xffdf3f;
export const OBJECT_SELECTED_COLOR = 0xffaa00;
export const NPC_COLOR = 0xff0000;
export const NPC_HOVER_COLOR = 0xff3f5f;
export const NPC_SELECTED_COLOR = 0xff0054;

export function create_object_mesh(object: QuestObject, geometry: BufferGeometry): Mesh {
    return create_mesh(object, geometry, OBJECT_COLOR, "Object");
}

export function create_npc_mesh(npc: QuestNpc, geometry: BufferGeometry): Mesh {
    return create_mesh(npc, geometry, NPC_COLOR, "NPC");
}

function create_mesh(
    entity: QuestEntity,
    geometry: BufferGeometry,
    color: number,
    type: string
): Mesh {
    const mesh = new Mesh(
        geometry,
        new MeshLambertMaterial({
            color,
            side: DoubleSide,
        })
    );
    mesh.name = type;
    mesh.userData.entity = entity;

    return mesh;
}
