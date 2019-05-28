import {
    createObjectMesh,
    createNpcMesh,
    OBJECT_COLOR,
    NPC_COLOR
} from './entities';
import { Object3D, Vector3, MeshLambertMaterial, CylinderBufferGeometry } from 'three';
import { Vec3, QuestNpc, QuestObject, Section, NpcType, ObjectType } from '../domain';
import { DatObject, DatNpc } from '../data/parsing/dat';

const cylinder = new CylinderBufferGeometry(3, 3, 20).translate(0, 10, 0);

test('create geometry for quest objects', () => {
    const object = new QuestObject(7, 13, new Vec3(17, 19, 23), new Vec3(), ObjectType.PrincipalWarp, {} as DatObject);
    const sectRot = 0.6;
    const sectRotSin = Math.sin(sectRot);
    const sectRotCos = Math.cos(sectRot);
    const geometry = createObjectMesh(
        object, [new Section(13, new Vec3(29, 31, 37), sectRot)], cylinder);

    expect(geometry).toBeInstanceOf(Object3D);
    expect(geometry.name).toBe('Object');
    expect(geometry.userData.entity).toBe(object);
    expect(geometry.position.x).toBe(sectRotCos * 17 + sectRotSin * 23 + 29);
    expect(geometry.position.y).toBe(19 + 31);
    expect(geometry.position.z).toBe(-sectRotSin * 17 + sectRotCos * 23 + 37);
    expect((geometry.material as MeshLambertMaterial).color.getHex()).toBe(OBJECT_COLOR);
});

test('create geometry for quest NPCs', () => {
    const npc = new QuestNpc(7, 13, new Vec3(17, 19, 23), new Vec3(), NpcType.Booma, {} as DatNpc);
    const sectRot = 0.6;
    const sectRotSin = Math.sin(sectRot);
    const sectRotCos = Math.cos(sectRot);
    const geometry = createNpcMesh(
        npc, [new Section(13, new Vec3(29, 31, 37), sectRot)], cylinder);

    expect(geometry).toBeInstanceOf(Object3D);
    expect(geometry.name).toBe('NPC');
    expect(geometry.userData.entity).toBe(npc);
    expect(geometry.position.x).toBe(sectRotCos * 17 + sectRotSin * 23 + 29);
    expect(geometry.position.y).toBe(19 + 31);
    expect(geometry.position.z).toBe(-sectRotSin * 17 + sectRotCos * 23 + 37);
    expect((geometry.material as MeshLambertMaterial).color.getHex()).toBe(NPC_COLOR);
});

test('geometry position changes when entity position changes element-wise', () => {
    const npc = new QuestNpc(7, 13, new Vec3(17, 19, 23), new Vec3(), NpcType.Booma, {} as DatNpc);
    const geometry = createNpcMesh(
        npc, [new Section(13, new Vec3(0, 0, 0), 0)], cylinder);
    npc.position = new Vec3(2, 3, 5).add(npc.position);

    expect(geometry.position).toEqual(new Vector3(19, 22, 28));
});

test('geometry position changes when entire entity position changes', () => {
    const npc = new QuestNpc(7, 13, new Vec3(17, 19, 23), new Vec3(), NpcType.Booma, {} as DatNpc);
    const geometry = createNpcMesh(
        npc, [new Section(13, new Vec3(0, 0, 0), 0)], cylinder);
    npc.position = new Vec3(2, 3, 5);

    expect(geometry.position).toEqual(new Vector3(2, 3, 5));
});
