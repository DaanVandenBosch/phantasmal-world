import { autorun } from 'mobx';
import { BufferGeometry, DoubleSide, Mesh, MeshLambertMaterial } from 'three';
import { QuestNpc, QuestObject, QuestEntity } from '../domain';

export const OBJECT_COLOR = 0xFFFF00;
export const OBJECT_HOVER_COLOR = 0xFFDF3F;
export const OBJECT_SELECTED_COLOR = 0xFFAA00;
export const NPC_COLOR = 0xFF0000;
export const NPC_HOVER_COLOR = 0xFF3F5F;
export const NPC_SELECTED_COLOR = 0xFF0054;

export function createObjectMesh(object: QuestObject, geometry: BufferGeometry): Mesh {
    return createMesh(object, geometry, OBJECT_COLOR, 'Object');
}

export function createNpcMesh(npc: QuestNpc, geometry: BufferGeometry): Mesh {
    return createMesh(npc, geometry, NPC_COLOR, 'NPC');
}

function createMesh(
    entity: QuestEntity,
    geometry: BufferGeometry,
    color: number,
    type: string
): Mesh {
    const object3d = new Mesh(
        geometry,
        new MeshLambertMaterial({
            color,
            side: DoubleSide
        })
    );
    object3d.name = type;
    object3d.userData.entity = entity;

    // TODO: dispose autorun?
    autorun(() => {
        const { x, y, z } = entity.position;
        object3d.position.set(x, y, z);
        const rot = entity.rotation;
        object3d.rotation.set(rot.x, rot.y, rot.z);
    });

    return object3d;
}
