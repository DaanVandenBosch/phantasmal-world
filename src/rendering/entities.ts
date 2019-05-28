import { BufferGeometry, CylinderGeometry, DoubleSide, Mesh, MeshLambertMaterial } from 'three';
import { autorun } from 'mobx';
import { Vec3, VisibleQuestEntity, QuestNpc, QuestObject, Section } from '../domain';

export const OBJECT_COLOR = 0xFFFF00;
export const OBJECT_HOVER_COLOR = 0xFFDF3F;
export const OBJECT_SELECTED_COLOR = 0xFFAA00;
export const NPC_COLOR = 0xFF0000;
export const NPC_HOVER_COLOR = 0xFF3F5F;
export const NPC_SELECTED_COLOR = 0xFF0054;

export function create_object_mesh(object: QuestObject, sections: Section[], geometry: BufferGeometry): Mesh {
    return create_mesh(object, sections, geometry, OBJECT_COLOR, 'Object');
}

export function create_npc_mesh(npc: QuestNpc, sections: Section[], geometry: BufferGeometry): Mesh {
    return create_mesh(npc, sections, geometry, NPC_COLOR, 'NPC');
}

const cylinder = new CylinderGeometry(3, 3, 20).translate(0, 10, 0);

function create_mesh(
    entity: VisibleQuestEntity,
    sections: Section[],
    geometry: BufferGeometry,
    color: number,
    type: string
): Mesh {
    let {x, y, z} = entity.position;

    const section = sections.find(s => s.id === entity.section_id);
    entity.section = section;

    if (section) {
        const {x: sec_x, y: sec_y, z: sec_z} = section.position;
        const rot_x = section.cos_y_axis_rotation * x + section.sin_y_axis_rotation * z;
        const rot_z = -section.sin_y_axis_rotation * x + section.cos_y_axis_rotation * z;
        x = rot_x + sec_x;
        y += sec_y;
        z = rot_z + sec_z;
    } else {
        console.warn(`Section ${entity.section_id} not found.`);
    }

    const object_3d = new Mesh(
        geometry || cylinder,
        new MeshLambertMaterial({
            color,
            side: DoubleSide
        })
    );
    object_3d.name = type;
    object_3d.userData.entity = entity;

    // TODO: dispose autorun?
    autorun(() => {
        const {x, y, z} = entity.position;
        object_3d.position.set(x, y, z);
        const rot = entity.rotation;
        object_3d.rotation.set(rot.x, rot.y, rot.z);
    });

    entity.position = new Vec3(x, y, z);

    return object_3d;
}
