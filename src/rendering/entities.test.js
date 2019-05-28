import {
    create_object_mesh,
    create_npc_mesh,
    OBJECT_COLOR,
    NPC_COLOR
} from './entities';
import { Object3D, Vector3 } from 'three';
import { Vec3, QuestNpc, QuestObject, Section, NpcType, ObjectType } from '../domain';

test('create geometry for quest objects', () => {
    const object = new QuestObject(7, 13, new Vec3(17, 19, 23), new Vec3(), ObjectType.PrincipalWarp, null);
    const sect_rot = 0.6;
    const sect_rot_sin = Math.sin(sect_rot);
    const sect_rot_cos = Math.cos(sect_rot);
    const geometry = create_object_mesh(
        object, [new Section(13, new Vec3(29, 31, 37), sect_rot)]);

    expect(geometry).toBeInstanceOf(Object3D);
    expect(geometry.name).toBe('Object');
    expect(geometry.userData.entity).toBe(object);
    expect(geometry.position.x).toBe(sect_rot_cos * 17 + sect_rot_sin * 23 + 29);
    expect(geometry.position.y).toBe(19 + 31);
    expect(geometry.position.z).toBe(-sect_rot_sin * 17 + sect_rot_cos * 23 + 37);
    expect(geometry.material.color.getHex()).toBe(OBJECT_COLOR);
});

test('create geometry for quest NPCs', () => {
    const npc = new QuestNpc(7, 13, new Vec3(17, 19, 23), new Vec3(), NpcType.Booma, null);
    const sect_rot = 0.6;
    const sect_rot_sin = Math.sin(sect_rot);
    const sect_rot_cos = Math.cos(sect_rot);
    const geometry = create_npc_mesh(
        npc, [new Section(13, new Vec3(29, 31, 37), sect_rot)]);

    expect(geometry).toBeInstanceOf(Object3D);
    expect(geometry.name).toBe('NPC');
    expect(geometry.userData.entity).toBe(npc);
    expect(geometry.position.x).toBe(sect_rot_cos * 17 + sect_rot_sin * 23 + 29);
    expect(geometry.position.y).toBe(19 + 31);
    expect(geometry.position.z).toBe(-sect_rot_sin * 17 + sect_rot_cos * 23 + 37);
    expect(geometry.material.color.getHex()).toBe(NPC_COLOR);
});

test('geometry position changes when entity position changes element-wise', () => {
    const npc = new QuestNpc(7, 13, new Vec3(17, 19, 23), new Vec3(), NpcType.Booma, null);
    const geometry = create_npc_mesh(
        npc, [new Section(13, new Vec3(0, 0, 0), 0)]);
    npc.position = new Vec3(2, 3, 5).add(npc.position);

    expect(geometry.position).toEqual(new Vector3(19, 22, 28));
});

test('geometry position changes when entire entity position changes', () => {
    const npc = new QuestNpc(7, 13, new Vec3(17, 19, 23), new Vec3(), NpcType.Booma, null);
    const geometry = create_npc_mesh(
        npc, [new Section(13, new Vec3(0, 0, 0), 0)]);
    npc.position = new Vec3(2, 3, 5);

    expect(geometry.position).toEqual(new Vector3(2, 3, 5));
});
